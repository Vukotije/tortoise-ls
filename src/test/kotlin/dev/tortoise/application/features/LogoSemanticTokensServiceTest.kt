package dev.tortoise.application.features

import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.DocumentSnapshot
import dev.tortoise.shared.model.LogoSemanticTokenType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogoSemanticTokensServiceTest {
    private val analysisService = CachedAnalysisService()
    private val semanticTokensService = LogoSemanticTokensService()

    @Test
    fun `classify aliases predicates and thing word reads`() {
        val source = """
            define "star [[n][fd :n]]
            make "distance 10
            st
            shown?
            word? "hello
            show thing "distance
        """.trimIndent()

        val analysis = analysisService.analyze(DocumentSnapshot("file:///tokens.logo", 1, source))
        val tokens = semanticTokensService.semanticTokens(analysis).tokens
        val byLexeme = tokens.groupBy { token ->
            source.substring(token.span.start.offset, token.span.end.offset)
        }

        assertTrue(byLexeme["st"].orEmpty().any { it.type == LogoSemanticTokenType.BUILT_IN })
        assertTrue(byLexeme["shown?"].orEmpty().any { it.type == LogoSemanticTokenType.BUILT_IN })
        assertTrue(byLexeme["word?"].orEmpty().any { it.type == LogoSemanticTokenType.BUILT_IN })
        assertTrue(byLexeme["\"star"].orEmpty().any { it.type == LogoSemanticTokenType.PROCEDURE })
        assertTrue(byLexeme["\"distance"].orEmpty().any { it.type == LogoSemanticTokenType.VARIABLE })
    }
}
