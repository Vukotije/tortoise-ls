package dev.tortoise.server.protocol

import dev.tortoise.shared.model.LogoSemanticToken
import dev.tortoise.shared.model.LogoSemanticTokenLegend
import dev.tortoise.shared.model.LogoSemanticTokens
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensLegend

object SemanticTokensProtocolMapper {
    private const val NO_TOKEN_MODIFIERS = 0

    fun toLspLegend(legend: LogoSemanticTokenLegend): SemanticTokensLegend {
        return SemanticTokensLegend(legend.tokenTypes, emptyList())
    }

    fun toLspSemanticTokens(semanticTokens: LogoSemanticTokens): SemanticTokens {
        val tokenTypeIndexes = semanticTokens.legend.tokenTypes
            .withIndex()
            .associate { (index, tokenType) -> tokenType to index }
        val data = mutableListOf<Int>()
        var previousLine = 0
        var previousStartCharacter = 0

        semanticTokens.tokens.forEach { token ->
            if (!token.isValidForLsp()) {
                return@forEach
            }

            val line = token.span.start.line - 1
            val startCharacter = token.span.start.column - 1
            val length = token.span.end.column - token.span.start.column
            val tokenTypeIndex = tokenTypeIndexes[token.type.legendName] ?: return@forEach
            val deltaLine = line - previousLine
            val deltaStartCharacter = if (deltaLine == 0) {
                startCharacter - previousStartCharacter
            } else {
                startCharacter
            }

            data += deltaLine
            data += deltaStartCharacter
            data += length
            data += tokenTypeIndex
            data += NO_TOKEN_MODIFIERS

            previousLine = line
            previousStartCharacter = startCharacter
        }

        return SemanticTokens(data)
    }

    private fun LogoSemanticToken.isValidForLsp(): Boolean {
        return span.start.line == span.end.line &&
            span.start.column < span.end.column &&
            span.start.line >= 1 &&
            span.start.column >= 1
    }
}
