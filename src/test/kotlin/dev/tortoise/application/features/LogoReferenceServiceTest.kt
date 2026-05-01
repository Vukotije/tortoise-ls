package dev.tortoise.application.features

import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.DocumentSnapshot
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LogoReferenceServiceTest {
    private val analysisService = CachedAnalysisService()
    private val referenceService = LogoReferenceService()

    @Test
    fun `find references for user procedure that collides with built-in name`() {
        val source = """
            to forward
              right 90
            end

            forward
            for<caret>ward
        """.trimIndent()

        val marker = source.withCaret()
        val analysis = analysisService.analyze(DocumentSnapshot("file:///references.logo", 1, marker.source))
        val references = referenceService.references(analysis, marker.position, includeDeclaration = true)

        assertEquals(
            listOf("1:4-1:11", "5:1-5:8", "6:1-6:8"),
            references.map { span -> span.format() },
        )
    }

    @Test
    fun `find references for thing word literal variable read`() {
        val source = """
            to demo
              localmake "distance 10
              show thing "distance
              show thing "dist<caret>ance
            end
        """.trimIndent()

        val marker = source.withCaret()
        val analysis = analysisService.analyze(DocumentSnapshot("file:///references.logo", 1, marker.source))
        val references = referenceService.references(analysis, marker.position, includeDeclaration = true)

        assertEquals(
            listOf("2:13-2:22", "3:14-3:23", "4:14-4:23"),
            references.map { span -> span.format() },
        )
    }

    @Test
    fun `find references for procedure declared with Turtle Academy define list form`() {
        val source = """
            define "star [[n][repeat 5 [fd :n rt 144]]]

            star 50
            st<caret>ar 75
        """.trimIndent()

        val marker = source.withCaret()
        val analysis = analysisService.analyze(DocumentSnapshot("file:///references.logo", 1, marker.source))
        val references = referenceService.references(analysis, marker.position, includeDeclaration = true)

        assertEquals(
            listOf("1:8-1:13", "3:1-3:5", "4:1-4:5"),
            references.map { span -> span.format() },
        )
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

    private data class SourceWithCaret(
        val source: String,
        val position: SourcePosition,
    )

    private companion object {
        const val CARET = "<caret>"
    }
}
