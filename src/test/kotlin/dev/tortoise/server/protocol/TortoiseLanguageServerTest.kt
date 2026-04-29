package dev.tortoise.server.protocol

import dev.tortoise.application.documents.FullTextSyncStrategy
import dev.tortoise.application.documents.InMemoryDocumentStore
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TortoiseLanguageServerTest {
    @Test
    fun `initialize returns minimal server capabilities`() {
        val server = TortoiseLanguageServer()

        val result = server.initialize(InitializeParams()).get()

        assertNotNull(result.capabilities)
        assertEquals(TextDocumentSyncKind.Full, result.capabilities.textDocumentSync.left)
        assertNull(result.capabilities.completionProvider)
        assertNull(result.capabilities.definitionProvider)
        assertNull(result.capabilities.referencesProvider)
        assertNull(result.capabilities.semanticTokensProvider)
    }

    @Test
    fun `text document lifecycle updates document store`() {
        val store = InMemoryDocumentStore(FullTextSyncStrategy())
        val server = TortoiseLanguageServer(store)
        val service = server.textDocumentService
        val uri = "file:///shape.logo"

        service.didOpen(
            DidOpenTextDocumentParams(
                TextDocumentItem(uri, "logo", 1, "forward 10"),
            ),
        )

        assertEquals("forward 10", store.get(uri)?.text)
        assertEquals(1, store.get(uri)?.version)

        service.didChange(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier(uri, 2),
                listOf(TextDocumentContentChangeEvent("right 90")),
            ),
        )

        assertEquals("right 90", store.get(uri)?.text)
        assertEquals(2, store.get(uri)?.version)

        service.didClose(DidCloseTextDocumentParams(TextDocumentIdentifier(uri)))

        assertNull(store.get(uri))
    }
}
