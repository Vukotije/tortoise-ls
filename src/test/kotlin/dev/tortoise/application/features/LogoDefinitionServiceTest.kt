package dev.tortoise.application.features

import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.DocumentSnapshot
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LogoDefinitionServiceTest {
    private val analysisService = CachedAnalysisService()
    private val definitionService = LogoDefinitionService()

    @Test
    fun `find definition for procedure call position`() {
        val source = """
            to square :size
              forward :size
            end

            sq<caret>uare 10
        """.trimIndent()

        val result = definitionAt(source)

        assertEquals("square", result.targetLexeme)
        assertEquals("1:4-1:10", result.targetRange)
    }

    @Test
    fun `find definition for parameter reference position`() {
        val source = """
            to square :size
              forward :si<caret>ze
            end
        """.trimIndent()

        val result = definitionAt(source)

        assertEquals(":size", result.targetLexeme)
        assertEquals("1:11-1:16", result.targetRange)
    }

    @Test
    fun `find definition for variable read position`() {
        val source = """
            to demo
              localmake "distance 10
              forward :dist<caret>ance
            end
        """.trimIndent()

        val result = definitionAt(source)

        assertEquals("\"distance", result.targetLexeme)
        assertEquals("2:13-2:22", result.targetRange)
    }

    @Test
    fun `return no definition for built-in procedure position`() {
        val source = """
            to demo
              for<caret>ward 10
            end
        """.trimIndent()

        val marker = source.withCaret()
        val analysis = analysisService.analyze(DocumentSnapshot("file:///definition.logo", 1, marker.source))

        assertNull(definitionService.definition(analysis, marker.position))
    }

    @Test
    fun `find definition for user procedure that collides with built-in name`() {
        val source = """
            to forward
              right 90
            end

            for<caret>ward
        """.trimIndent()

        val result = definitionAt(source)

        assertEquals("forward", result.targetLexeme)
        assertEquals("1:4-1:11", result.targetRange)
    }

    @Test
    fun `find definition for thing word literal variable read`() {
        val source = """
            to demo
              localmake "distance 10
              show thing "dist<caret>ance
            end
        """.trimIndent()

        val result = definitionAt(source)

        assertEquals("\"distance", result.targetLexeme)
        assertEquals("2:13-2:22", result.targetRange)
    }

    @Test
    fun `find definition for procedure declared with Turtle Academy define list form`() {
        val source = """
            define "star [[n][repeat 5 [fd :n rt 144]]]

            st<caret>ar 50
        """.trimIndent()

        val result = definitionAt(source)

        assertEquals("\"star", result.targetLexeme)
        assertEquals("1:8-1:13", result.targetRange)
    }

    private fun definitionAt(sourceWithCaret: String): DefinitionResult {
        val marker = sourceWithCaret.withCaret()
        val analysis = analysisService.analyze(DocumentSnapshot("file:///definition.logo", 1, marker.source))
        val span = definitionService.definition(analysis, marker.position)
            ?: error("Expected definition at ${marker.position}.")
        return DefinitionResult(
            targetLexeme = marker.source.substring(span.start.offset, span.end.offset),
            targetRange = span.format(),
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

    private data class DefinitionResult(
        val targetLexeme: String,
        val targetRange: String,
    )

    private companion object {
        const val CARET = "<caret>"
    }
}
