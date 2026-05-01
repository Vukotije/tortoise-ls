package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.language.lexer.LogoToken
import dev.tortoise.language.lexer.LogoTokenType
import dev.tortoise.language.symbols.LogoVariableSymbolKind
import dev.tortoise.shared.model.LogoSemanticToken
import dev.tortoise.shared.model.LogoSemanticTokenLegend
import dev.tortoise.shared.model.LogoSemanticTokenType
import dev.tortoise.shared.model.LogoSemanticTokens

class LogoSemanticTokensService : SemanticTokensService {
    override fun semanticTokens(analysis: DocumentAnalysis): LogoSemanticTokens {
        val procedureReferencesByStart = analysis.resolutionResult.procedureReferences.associateBy {
            it.span.start.offset
        }
        val variableReferencesByStart = analysis.resolutionResult.variableReferences.associateBy {
            it.span.start.offset
        }
        val variableDeclarationsByStart = analysis.resolutionResult.variableDeclarations.associateBy {
            it.declarationSpan.start.offset
        }

        val tokens = analysis.parseResult.tokens.mapNotNull { token ->
            val type = when (token.type) {
                LogoTokenType.KEYWORD_TO,
                LogoTokenType.KEYWORD_DEFINE,
                LogoTokenType.KEYWORD_END,
                LogoTokenType.KEYWORD_REPEAT,
                LogoTokenType.KEYWORD_FOR,
                LogoTokenType.KEYWORD_IF,
                LogoTokenType.KEYWORD_IFELSE,
                LogoTokenType.KEYWORD_DOTIMES,
                LogoTokenType.KEYWORD_WHILE,
                LogoTokenType.KEYWORD_UNTIL,
                LogoTokenType.KEYWORD_MAKE,
                LogoTokenType.KEYWORD_LOCALMAKE,
                LogoTokenType.KEYWORD_NAME,
                -> LogoSemanticTokenType.KEYWORD

                LogoTokenType.NUMBER -> LogoSemanticTokenType.NUMBER
                LogoTokenType.VARIABLE_REFERENCE -> (
                    variableDeclarationsByStart[token.span.start.offset]?.kind
                        ?: variableReferencesByStart[token.span.start.offset]?.declaration?.kind
                    ).toVariableTokenType()

                LogoTokenType.WORD_LITERAL -> variableDeclarationsByStart[token.span.start.offset]
                    ?.kind
                    .toVariableTokenType()

                LogoTokenType.IDENTIFIER -> classifyIdentifier(
                    token = token,
                    procedureReferencesByStart = procedureReferencesByStart,
                    analysis = analysis,
                )

                else -> null
            }

            type?.let { LogoSemanticToken(type = it, span = token.span) }
        }

        return LogoSemanticTokens(
            legend = LEGEND,
            tokens = tokens.sortedWith(compareBy({ it.span.start.offset }, { it.span.end.offset }, { it.type.legendName })),
        )
    }

    private fun classifyIdentifier(
        token: LogoToken,
        procedureReferencesByStart: Map<Int, dev.tortoise.language.resolve.LogoProcedureReferenceBinding>,
        analysis: DocumentAnalysis,
    ): LogoSemanticTokenType? {
        val procedureReference = procedureReferencesByStart[token.span.start.offset]
        if (procedureReference != null) {
            return when {
                procedureReference.isBuiltIn -> LogoSemanticTokenType.BUILT_IN
                procedureReference.declaration != null -> LogoSemanticTokenType.PROCEDURE
                else -> null
            }
        }

        return if (isProcedureDeclarationName(token, analysis.parseResult.tokens)) {
            LogoSemanticTokenType.PROCEDURE
        } else {
            null
        }
    }

    private fun isProcedureDeclarationName(token: LogoToken, tokens: List<LogoToken>): Boolean {
        val tokenIndex = tokens.indexOf(token)
        if (tokenIndex <= 0) {
            return false
        }

        val previousToken = tokens[tokenIndex - 1]
        return previousToken.type == LogoTokenType.KEYWORD_TO ||
            previousToken.type == LogoTokenType.KEYWORD_DEFINE
    }

    private fun LogoVariableSymbolKind?.toVariableTokenType(): LogoSemanticTokenType? {
        return when (this) {
            LogoVariableSymbolKind.PARAMETER -> LogoSemanticTokenType.PARAMETER
            LogoVariableSymbolKind.MAKE_NAME,
            LogoVariableSymbolKind.LOCALMAKE,
            LogoVariableSymbolKind.LOOP,
            -> LogoSemanticTokenType.VARIABLE

            null -> null
        }
    }

    private companion object {
        val LEGEND = LogoSemanticTokenLegend(
            tokenTypes = LogoSemanticTokenType.entries.map { it.legendName },
        )
    }
}
