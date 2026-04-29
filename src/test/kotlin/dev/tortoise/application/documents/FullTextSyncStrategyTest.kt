package dev.tortoise.application.documents

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FullTextSyncStrategyTest {
    private val strategy = FullTextSyncStrategy()

    @Test
    fun `apply replaces entire document text`() {
        val current = DocumentSnapshot(
            uri = "file:///shape.logo",
            version = 1,
            text = "forward 10",
        )

        val updated = strategy.apply(
            current = current,
            newVersion = 2,
            changes = listOf(TextDocumentChange("left 90")),
        )

        assertEquals(DocumentSnapshot("file:///shape.logo", 2, "left 90"), updated)
    }

    @Test
    fun `apply requires replacement text`() {
        val current = DocumentSnapshot(
            uri = "file:///shape.logo",
            version = 1,
            text = "forward 10",
        )

        assertThrows(IllegalArgumentException::class.java) {
            strategy.apply(current, newVersion = 2, changes = emptyList())
        }
    }
}
