package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.language.lexer.LogoToken
import dev.tortoise.language.lexer.LogoTokenType
import dev.tortoise.language.symbols.LogoProcedureSymbol
import dev.tortoise.language.symbols.LogoVariableSymbol
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

class LogoReferenceService : ReferenceService {
    override fun references(
        analysis: DocumentAnalysis,
        position: SourcePosition,
        includeDeclaration: Boolean,
    ): List<SourceSpan> {
        val token = analysis.tokenAt(position) ?: return emptyList()
        return when (token.type) {
            LogoTokenType.IDENTIFIER -> procedureReferences(analysis, token, includeDeclaration)
            LogoTokenType.VARIABLE_REFERENCE,
            LogoTokenType.WORD_LITERAL,
            -> variableReferences(analysis, token, includeDeclaration)

            else -> emptyList()
        }
    }

    private fun procedureReferences(
        analysis: DocumentAnalysis,
        token: LogoToken,
        includeDeclaration: Boolean,
    ): List<SourceSpan> {
        val declaration = procedureSymbolAt(analysis, token) ?: return emptyList()
        val spans = mutableListOf<SourceSpan>()
        if (includeDeclaration) {
            spans += analysis.procedureNameSpan(declaration) ?: declaration.declarationSpan
        }

        spans += analysis.resolutionResult.procedureReferences
            .filter { reference -> !reference.isBuiltIn && reference.declaration == declaration }
            .mapNotNull { reference -> analysis.tokenAtStartOffset(reference.span.start.offset)?.span }

        return spans.sortedDistinct()
    }

    private fun variableReferences(
        analysis: DocumentAnalysis,
        token: LogoToken,
        includeDeclaration: Boolean,
    ): List<SourceSpan> {
        val declaration = variableSymbolAt(analysis, token) ?: return emptyList()
        val spans = mutableListOf<SourceSpan>()
        if (includeDeclaration) {
            spans += declaration.declarationSpan
        }

        spans += analysis.resolutionResult.variableReferences
            .filter { reference -> reference.declaration == declaration }
            .map { reference -> reference.span }

        return spans.sortedDistinct()
    }

    private fun procedureSymbolAt(analysis: DocumentAnalysis, token: LogoToken): LogoProcedureSymbol? {
        val reference = analysis.resolutionResult.procedureReferences
            .firstOrNull { reference -> reference.span.start.offset == token.span.start.offset }
        if (reference != null) {
            return if (reference.isBuiltIn) null else reference.declaration
        }

        val declaration = analysis.resolutionResult.procedureTable.resolve(token.lexeme) ?: return null
        return if (analysis.procedureNameSpan(declaration) == token.span) {
            declaration
        } else {
            null
        }
    }

    private fun variableSymbolAt(analysis: DocumentAnalysis, token: LogoToken): LogoVariableSymbol? {
        val reference = analysis.resolutionResult.variableReferences
            .firstOrNull { reference -> reference.span.start.offset == token.span.start.offset }
        if (reference != null) {
            return reference.declaration
        }

        return analysis.resolutionResult.variableDeclarations.firstOrNull { declaration ->
            declaration.declarationSpan == token.span
        }
    }

    private fun List<SourceSpan>.sortedDistinct(): List<SourceSpan> {
        return distinctBy { span -> span.start.offset to span.end.offset }
            .sortedWith(compareBy({ it.start.offset }, { it.end.offset }))
    }
}
