package dev.tortoise.language.lexer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.nio.file.Files
import java.nio.file.Path

class TurtleLogoLexerGoldenTest {
    private val lexer = TurtleLogoLexer()
    private val fixturesDirectory: Path = Path.of("src", "test", "resources", "fixtures", "lexer")

    @TestFactory
    fun `golden lexer fixtures`(): List<DynamicTest> {
        val fixtures = Files.newDirectoryStream(fixturesDirectory, "*.logo").use { directoryStream ->
            directoryStream.map { it }.sortedBy { it.fileName.toString() }
        }
        assertTrue(fixtures.isNotEmpty(), "Expected at least one lexer fixture.")

        return fixtures.map { sourcePath ->
            dynamicTest(sourcePath.fileName.toString()) {
                val source = Files.readString(sourcePath)
                val expectedPath = sourcePath.resolveSibling(
                    sourcePath.fileName.toString().removeSuffix(".logo") + ".tokens",
                )
                assertTrue(Files.exists(expectedPath), "Missing golden file: $expectedPath")

                val expected = normalizeLineEndings(Files.readString(expectedPath)).trimEnd()
                val actual = normalizeLineEndings(formatTokens(lexer.tokenize(source))).trimEnd()
                assertEquals(expected, actual)
            }
        }
    }

    private fun formatTokens(tokens: List<LogoToken>): String {
        return tokens.joinToString(separator = "\n") { token ->
            val lexeme = if (token.type == LogoTokenType.NEWLINE) "\n" else token.lexeme
            "${token.type}|${escapeLexeme(lexeme)}|${formatSpan(token.span)}"
        }
    }

    private fun formatSpan(span: dev.tortoise.shared.text.SourceSpan): String {
        return "${span.start.line}:${span.start.column}-${span.end.line}:${span.end.column}"
    }

    private fun escapeLexeme(lexeme: String): String {
        return lexeme
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun normalizeLineEndings(value: String): String {
        return value.replace("\r\n", "\n").replace("\r", "\n")
    }
}
