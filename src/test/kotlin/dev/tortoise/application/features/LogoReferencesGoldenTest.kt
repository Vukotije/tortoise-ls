package dev.tortoise.application.features

import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.DocumentSnapshot
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class LogoReferencesGoldenTest {
    private val analysisService = CachedAnalysisService()
    private val referenceService = LogoReferenceService()
    private val fixturesDirectory: Path = Path.of("src", "test", "resources", "fixtures", "references")

    @TestFactory
    fun `golden reference fixtures`(): List<DynamicTest> {
        val fixtures = Files.newDirectoryStream(fixturesDirectory, "*.logo").use { directoryStream ->
            directoryStream.map { it }.sortedBy { it.fileName.toString() }
        }
        assertTrue(fixtures.isNotEmpty(), "Expected at least one reference fixture.")

        return fixtures.map { sourcePath ->
            dynamicTest(sourcePath.fileName.toString()) {
                val marker = normalizeLineEndings(Files.readString(sourcePath)).withCaret()
                val expectedPath = sourcePath.resolveSibling(
                    sourcePath.fileName.toString().removeSuffix(".logo") + ".references",
                )
                assertTrue(Files.exists(expectedPath), "Missing golden file: $expectedPath")

                val analysis = analysisService.analyze(
                    DocumentSnapshot(
                        uri = sourcePath.toUri().toString(),
                        version = 1,
                        text = marker.source,
                    ),
                )

                val expected = normalizeLineEndings(Files.readString(expectedPath)).trimEnd()
                val actual = formatReferences(
                    source = marker.source,
                    spans = referenceService.references(
                        analysis = analysis,
                        position = marker.position,
                        includeDeclaration = true,
                    ),
                ).trimEnd()
                assertEquals(expected, actual)
            }
        }
    }

    private fun formatReferences(source: String, spans: List<SourceSpan>): String {
        return spans.joinToString(separator = "\n") { span ->
            "${span.format()}|${source.substring(span.start.offset, span.end.offset)}"
        }
    }

    private fun String.withCaret(): SourceWithCaret {
        val offset = indexOf(CARET)
        require(offset >= 0) { "Missing $CARET marker." }
        val source = replace(CARET, "")
        return SourceWithCaret(source = source, position = source.positionAt(offset))
    }

    private fun String.positionAt(offset: Int): SourcePosition {
        var line = 1
        var column = 1
        for (index in 0 until offset) {
            if (this[index] == '\n') {
                line += 1
                column = 1
            } else {
                column += 1
            }
        }
        return SourcePosition(offset = offset, line = line, column = column)
    }

    private fun SourceSpan.format(): String {
        return "${start.line}:${start.column}-${end.line}:${end.column}"
    }

    private fun normalizeLineEndings(value: String): String {
        return value.replace("\r\n", "\n").replace("\r", "\n")
    }

    private data class SourceWithCaret(
        val source: String,
        val position: SourcePosition,
    )

    private companion object {
        const val CARET = "<caret>"
    }
}
