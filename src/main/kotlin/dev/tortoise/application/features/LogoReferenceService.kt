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
        val token = analysis.parseResult.tokens.firstOrNull { it.span.contains(position) } ?: return emptyList()
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
            spans += procedureNameSpan(analysis, declaration) ?: declaration.declarationSpan
        }

        spans += analysis.resolutionResult.procedureReferences
            .filter { reference -> !reference.isBuiltIn && reference.declaration == declaration }
            .mapNotNull { reference -> tokenAtStartOffset(analysis, reference.span.start.offset)?.span }

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
        return if (procedureNameSpan(analysis, declaration) == token.span) {
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

    private fun procedureNameSpan(analysis: DocumentAnalysis, declaration: LogoProcedureSymbol): SourceSpan? {
        val tokens = analysis.parseResult.tokens
        return tokens.withIndex().firstOrNull { (index, token) ->
            token.type == LogoTokenType.IDENTIFIER &&
                token.lexeme.equals(declaration.name, ignoreCase = true) &&
                token.span.isInside(declaration.declarationSpan) &&
                index > 0 &&
                tokens[index - 1].type.isProcedureDeclarationKeyword()
        }?.value?.span
    }

    private fun tokenAtStartOffset(analysis: DocumentAnalysis, offset: Int): LogoToken? {
        return analysis.parseResult.tokens.firstOrNull { token -> token.span.start.offset == offset }
    }

    private fun List<SourceSpan>.sortedDistinct(): List<SourceSpan> {
        return distinctBy { span -> span.start.offset to span.end.offset }
            .sortedWith(compareBy({ it.start.offset }, { it.end.offset }))
    }

    private fun SourceSpan.contains(position: SourcePosition): Boolean {
        return position.offset >= start.offset && position.offset < end.offset
    }

    private fun SourceSpan.isInside(container: SourceSpan): Boolean {
        return start.offset >= container.start.offset && end.offset <= container.end.offset
    }

    private fun LogoTokenType.isProcedureDeclarationKeyword(): Boolean {
        return this == LogoTokenType.KEYWORD_TO || this == LogoTokenType.KEYWORD_DEFINE
    }
}
