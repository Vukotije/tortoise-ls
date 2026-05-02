package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.language.lexer.LogoToken
import dev.tortoise.language.lexer.LogoTokenType
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

class LogoDefinitionService : DefinitionService {
    override fun definition(analysis: DocumentAnalysis, position: SourcePosition): SourceSpan? {
        val token = analysis.tokenAt(position) ?: return null
        return when (token.type) {
            LogoTokenType.IDENTIFIER -> procedureDefinition(analysis, token)
            LogoTokenType.VARIABLE_REFERENCE,
            LogoTokenType.WORD_LITERAL,
            -> variableDefinition(analysis, token)
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
        return analysis.procedureNameSpan(declaration) ?: declaration.declarationSpan
    }

    private fun variableDefinition(analysis: DocumentAnalysis, token: LogoToken): SourceSpan? {
        return analysis.resolutionResult.variableReferences
            .firstOrNull { it.span.start.offset == token.span.start.offset }
            ?.declaration
            ?.declarationSpan
    }
}
