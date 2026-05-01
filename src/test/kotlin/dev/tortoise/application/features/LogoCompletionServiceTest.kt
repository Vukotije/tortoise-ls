package dev.tortoise.application.features

import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.DocumentSnapshot
import dev.tortoise.shared.model.LogoCompletionItemKind
import dev.tortoise.shared.text.SourcePosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogoCompletionServiceTest {
    private val analysisService = CachedAnalysisService()
    private val completionService = LogoCompletionService()

    @Test
    fun `complete built-ins procedures parameters and visible variables in deterministic order`() {
        val source = """
            make "global 1

            to square :size
              localmake "distance 10
              repeat 4 [
                localmake "angle 90
                <caret>
              ]
            end

            to spiral
            end
        """.trimIndent()

        val result = completionsAt(source)
        val labels = result.map { it.label }

        assertTrue(labels.indexOf("arc") < labels.indexOf("spiral"))
        assertTrue(labels.indexOf("spiral") < labels.indexOf(":size"))
        assertTrue(labels.indexOf(":size") < labels.indexOf(":angle"))
        assertTrue(labels.indexOf(":angle") < labels.indexOf(":distance"))
        assertTrue(labels.indexOf(":distance") < labels.indexOf(":global"))
        assertEquals(labels, labels.distinct())
    }

    @Test
    fun `complete variables with colon prefix at procedure position`() {
        val source = """
            to square :size
              localmake "distance 10
              forward :s<caret>
            end
        """.trimIndent()

        val result = completionsAt(source)

        assertEquals(listOf(":size"), result.map { it.label })
        assertEquals(LogoCompletionItemKind.PARAMETER, result.single().kind)
    }

    @Test
    fun `complete user procedures with identifier prefix at file position`() {
        val source = """
            to square :size
              forward :size
            end

            to spiral
            end

            sq<caret>
        """.trimIndent()

        val result = completionsAt(source)

        assertEquals(listOf("square"), result.map { it.label })
        assertEquals(LogoCompletionItemKind.PROCEDURE, result.single().kind)
    }

    @Test
    fun `complete expanded Turtle Academy built-in catalog entries`() {
        val source = "<caret>"

        val labels = completionsAt(source).map { it.label }

        assertTrue("show" in labels)
        assertTrue("print" in labels)
        assertTrue("wait" in labels)
        assertTrue("cleartext" in labels)
        assertTrue("difference" in labels)
        assertTrue("repcount" in labels)
        assertTrue("thing" in labels)
        assertTrue("do.while" in labels)
    }

    @Test
    fun `complete user procedure instead of built-in when names collide`() {
        val source = """
            to forward
            end

            fo<caret>
        """.trimIndent()

        val result = completionsAt(source)
        val forward = result.single { it.label == "forward" }

        assertEquals(LogoCompletionItemKind.PROCEDURE, forward.kind)
    }

    @Test
    fun `complete dotted built-in with dotted prefix`() {
        val source = "do.<caret>"

        val result = completionsAt(source)

        assertEquals(listOf("do.until", "do.while"), result.map { it.label })
    }

    @Test
    fun `do not offer block local variables outside their block`() {
        val source = """
            to demo :size
              repeat 4 [
                localmake "angle 90
              ]
              :<caret>
            end
        """.trimIndent()

        val labels = completionsAt(source).map { it.label }

        assertTrue(":size" in labels)
        assertFalse(":angle" in labels)
    }

    private fun completionsAt(sourceWithCaret: String): List<dev.tortoise.shared.model.LogoCompletionItem> {
        val marker = sourceWithCaret.withCaret()
        val analysis = analysisService.analyze(DocumentSnapshot("file:///completion.logo", 1, marker.source))
        return completionService.completions(analysis, marker.position)
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

    private data class SourceWithCaret(
        val source: String,
        val position: SourcePosition,
    )

    private companion object {
        const val CARET = "<caret>"
    }
}
