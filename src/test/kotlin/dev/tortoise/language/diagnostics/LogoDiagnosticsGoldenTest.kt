package dev.tortoise.language.diagnostics

import dev.tortoise.language.parser.TurtleLogoParser
import dev.tortoise.language.resolve.TurtleLogoResolver
import dev.tortoise.shared.model.LogoDiagnostic
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class LogoDiagnosticsGoldenTest {
    private val parser = TurtleLogoParser()
    private val resolver = TurtleLogoResolver()
    private val diagnosticsService = LogoDiagnosticsService()
    private val fixturesDirectory: Path = Path.of("src", "test", "resources", "fixtures", "diagnostics")

    @TestFactory
    fun `golden diagnostics fixtures`(): List<DynamicTest> {
        val fixtures = Files.newDirectoryStream(fixturesDirectory, "*.logo").use { directoryStream ->
            directoryStream.map { it }.sortedBy { it.fileName.toString() }
        }
        assertTrue(fixtures.isNotEmpty(), "Expected at least one diagnostics fixture.")

        return fixtures.map { sourcePath ->
            dynamicTest(sourcePath.fileName.toString()) {
                val source = Files.readString(sourcePath)
                val expectedPath = sourcePath.resolveSibling(
                    sourcePath.fileName.toString().removeSuffix(".logo") + ".diagnostics",
                )
                assertTrue(Files.exists(expectedPath), "Missing golden file: $expectedPath")

                val expected = normalizeLineEndings(Files.readString(expectedPath)).trimEnd()
                val parseResult = parser.parse(source)
                val resolutionResult = resolver.resolve(parseResult.program)
                val actualDiagnostics = diagnosticsService.computeDiagnostics(parseResult, resolutionResult)
                val actual = normalizeLineEndings(formatDiagnostics(actualDiagnostics)).trimEnd()
                assertEquals(expected, actual)
            }
        }
    }

    private fun formatDiagnostics(diagnostics: List<LogoDiagnostic>): String {
        return diagnostics.joinToString(separator = "\n") { diagnostic ->
            val span = diagnostic.span
            "${span.start.line}:${span.start.column}-${span.end.line}:${span.end.column}|${diagnostic.code}|${diagnostic.message}"
        }
    }

    private fun normalizeLineEndings(value: String): String {
        return value.replace("\r\n", "\n").replace("\r", "\n")
    }
}
