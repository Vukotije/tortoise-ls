package dev.tortoise.application.documents

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class InMemoryDocumentStoreTest {
    private val store = InMemoryDocumentStore(FullTextSyncStrategy())

    @Test
    fun `open stores versioned document snapshot`() {
        val snapshot = store.open(
            uri = "file:///shape.logo",
            version = 1,
            text = "forward 10",
        )

        assertEquals(DocumentSnapshot("file:///shape.logo", 1, "forward 10"), snapshot)
        assertEquals(snapshot, store.get("file:///shape.logo"))
    }

    @Test
    fun `change replaces full text and version`() {
        store.open("file:///shape.logo", version = 1, text = "forward 10")

        val snapshot = store.change(
            uri = "file:///shape.logo",
            version = 2,
            changes = listOf(TextDocumentChange("right 90")),
        )

        assertEquals(DocumentSnapshot("file:///shape.logo", 2, "right 90"), snapshot)
        assertEquals(snapshot, store.get("file:///shape.logo"))
    }

    @Test
    fun `close removes document snapshot`() {
        store.open("file:///shape.logo", version = 1, text = "forward 10")

        store.close("file:///shape.logo")

        assertNull(store.get("file:///shape.logo"))
    }

    @Test
    fun `change requires document to be open`() {
        assertThrows(IllegalStateException::class.java) {
            store.change(
                uri = "file:///missing.logo",
                version = 2,
                changes = listOf(TextDocumentChange("forward 10")),
            )
        }
    }
}
