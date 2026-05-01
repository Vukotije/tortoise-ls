package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.language.lexer.LogoToken
import dev.tortoise.language.lexer.LogoTokenType
import dev.tortoise.language.symbols.LogoProcedureSymbol
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

class LogoDefinitionService : DefinitionService {
    override fun definition(analysis: DocumentAnalysis, position: SourcePosition): SourceSpan? {
        val token = analysis.parseResult.tokens.firstOrNull { it.span.contains(position) } ?: return null
        return when (token.type) {
            LogoTokenType.IDENTIFIER -> procedureDefinition(analysis, token)
            LogoTokenType.VARIABLE_REFERENCE -> variableDefinition(analysis, token)
            else -> null
        }
    }

    private fun procedureDefinition(analysis: DocumentAnalysis, token: LogoToken): SourceSpan? {
        val reference = analysis.resolutionResult.procedureReferences
            .firstOrNull { it.span.start.offset == token.span.start.offset }
            ?: return null

        if (reference.isBuiltIn) {
            return null
        }

        val declaration = reference.declaration ?: return null
        return procedureNameSpan(analysis, declaration) ?: declaration.declarationSpan
    }

    private fun variableDefinition(analysis: DocumentAnalysis, token: LogoToken): SourceSpan? {
        return analysis.resolutionResult.variableReferences
            .firstOrNull { it.span.start.offset == token.span.start.offset }
            ?.declaration
            ?.declarationSpan
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
