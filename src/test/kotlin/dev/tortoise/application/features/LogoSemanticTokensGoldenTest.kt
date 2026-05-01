package dev.tortoise.application.features

import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.DocumentSnapshot
import dev.tortoise.shared.model.LogoSemanticToken
import dev.tortoise.shared.model.LogoSemanticTokens
import dev.tortoise.shared.text.SourceSpan
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class LogoSemanticTokensGoldenTest {
    private val analysisService = CachedAnalysisService()
    private val semanticTokensService = LogoSemanticTokensService()
    private val fixturesDirectory: Path = Path.of("src", "test", "resources", "fixtures", "semantic-tokens")

    @TestFactory
    fun `golden semantic token fixtures`(): List<DynamicTest> {
        val fixtures = Files.newDirectoryStream(fixturesDirectory, "*.logo").use { directoryStream ->
            directoryStream.map { it }.sortedBy { it.fileName.toString() }
        }
        assertTrue(fixtures.isNotEmpty(), "Expected at least one semantic token fixture.")

        return fixtures.map { sourcePath ->
            dynamicTest(sourcePath.fileName.toString()) {
                val source = Files.readString(sourcePath)
                val expectedPath = sourcePath.resolveSibling(
                    sourcePath.fileName.toString().removeSuffix(".logo") + ".semantic-tokens",
                )
                assertTrue(Files.exists(expectedPath), "Missing golden file: $expectedPath")

                val expected = normalizeLineEndings(Files.readString(expectedPath)).trimEnd()
                val analysis = analysisService.analyze(
                    DocumentSnapshot(
                        uri = sourcePath.toUri().toString(),
                        version = 1,
                        text = source,
                    ),
                )
                val actual = normalizeLineEndings(
                    formatSemanticTokens(source, semanticTokensService.semanticTokens(analysis)),
                ).trimEnd()
                assertEquals(expected, actual)
            }
        }
    }

    private fun formatSemanticTokens(source: String, semanticTokens: LogoSemanticTokens): String {
        val legend = "legend|" + semanticTokens.legend.tokenTypes.joinToString(",")
        val tokens = semanticTokens.tokens.joinToString(separator = "\n") { token ->
            "${formatSpan(token.span)}|${token.type.legendName}|${source.lexeme(token)}"
        }
        return listOf(legend, tokens)
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")
    }

    private fun formatSpan(span: SourceSpan): String {
        return "${span.start.line}:${span.start.column}-${span.end.line}:${span.end.column}"
    }

    private fun String.lexeme(token: LogoSemanticToken): String {
        return substring(token.span.start.offset, token.span.end.offset)
    }

    private fun normalizeLineEndings(value: String): String {
        return value.replace("\r\n", "\n").replace("\r", "\n")
    }
}
