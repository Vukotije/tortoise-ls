package dev.tortoise.server.protocol

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.TextDocumentSyncKind
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
}
