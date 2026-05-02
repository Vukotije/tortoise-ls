package dev.tortoise.server.protocol

import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.FullTextSyncStrategy
import dev.tortoise.application.documents.InMemoryDocumentStore
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.services.LanguageClient
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TortoiseLanguageServerTest {
    @Test
    fun `initialize returns minimal server capabilities`() {
        val server = TortoiseLanguageServer()

        val result = server.initialize(InitializeParams()).get()

        assertNotNull(result.capabilities)
        assertEquals(TextDocumentSyncKind.Full, result.capabilities.textDocumentSync.left)
        assertNotNull(result.capabilities.completionProvider)
        assertEquals(false, result.capabilities.completionProvider.resolveProvider)
        assertEquals(true, result.capabilities.definitionProvider.left)
        assertEquals(true, result.capabilities.referencesProvider.left)
        assertNotNull(result.capabilities.semanticTokensProvider)
        assertEquals(
            listOf("keyword", "procedure", "builtin", "parameter", "variable", "number"),
            result.capabilities.semanticTokensProvider.legend.tokenTypes,
        )
        assertTrue(result.capabilities.semanticTokensProvider.full.left)
        assertFalse(result.capabilities.semanticTokensProvider.range.left)
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

    @Test
    fun `diagnostics are published on open and change`() {
        val store = InMemoryDocumentStore(FullTextSyncStrategy())
        val analysisService = CachedAnalysisService()
        val server = TortoiseLanguageServer(store, analysisService)
        val service = server.textDocumentService
        val published = mutableListOf<PublishDiagnosticsParams>()
        val uri = "file:///diagnostics.logo"

        server.connect(recordingClient(published))

        service.didOpen(
            DidOpenTextDocumentParams(
                TextDocumentItem(
                    uri,
                    "logo",
                    1,
                    """
                    to demo
                      missingProc
                    end
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(1, published.size)
        val firstPublish = published[0]
        assertEquals(uri, firstPublish.uri)
        assertEquals(1, firstPublish.version)
        assertTrue(firstPublish.diagnostics.any { diagnosticMessage(it).contains("Unknown procedure 'missingProc'") })

        service.didChange(
            DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier(uri, 2),
                listOf(
                    TextDocumentContentChangeEvent(
                        """
                        to demo
                          forward :ghost
                        end
                        """.trimIndent(),
                    ),
                ),
            ),
        )

        assertEquals(2, published.size)
        val secondPublish = published[1]
        assertEquals(uri, secondPublish.uri)
        assertEquals(2, secondPublish.version)
        assertTrue(secondPublish.diagnostics.any { diagnosticMessage(it).contains("Unknown variable ':ghost'") })
        assertFalse(secondPublish.diagnostics.any { diagnosticMessage(it).contains("Unknown procedure 'missingProc'") })
    }

    @Test
    fun `definition request returns procedure declaration location`() {
        val store = InMemoryDocumentStore(FullTextSyncStrategy())
        val server = TortoiseLanguageServer(store)
        val service = server.textDocumentService
        val uri = "file:///definition.logo"

        service.didOpen(
            DidOpenTextDocumentParams(
                TextDocumentItem(
                    uri,
                    "logo",
                    1,
                    """
                    to square :size
                      forward :size
                    end

                    square 10
                    """.trimIndent(),
                ),
            ),
        )

        val result = service.definition(
            DefinitionParams(
                TextDocumentIdentifier(uri),
                Position(4, 2),
            ),
        ).get()

        val location = result.left.single()
        assertEquals(uri, location.uri)
        assertEquals(Position(0, 3), location.range.start)
        assertEquals(Position(0, 9), location.range.end)
    }

    @Test
    fun `completion request returns deterministic items from cached analysis`() {
        val store = InMemoryDocumentStore(FullTextSyncStrategy())
        val server = TortoiseLanguageServer(store)
        val service = server.textDocumentService
        val uri = "file:///completion.logo"

        service.didOpen(
            DidOpenTextDocumentParams(
                TextDocumentItem(
                    uri,
                    "logo",
                    1,
                    """
                    to square :size
                      forward :s
                    end
                    """.trimIndent(),
                ),
            ),
        )

        val result = service.completion(
            CompletionParams(
                TextDocumentIdentifier(uri),
                Position(1, 12),
            ),
        ).get()

        val item = result.left.single()
        val textEdit = item.textEdit.left
        assertEquals(":size", item.label)
        assertNull(item.insertText)
        assertEquals(":size", textEdit.newText)
        assertEquals(Position(1, 10), textEdit.range.start)
        assertEquals(Position(1, 12), textEdit.range.end)
    }

    @Test
    fun `semantic tokens request returns lsp encoded tokens from cached analysis`() {
        val store = InMemoryDocumentStore(FullTextSyncStrategy())
        val server = TortoiseLanguageServer(store)
        val service = server.textDocumentService
        val uri = "file:///tokens.logo"

        service.didOpen(
            DidOpenTextDocumentParams(
                TextDocumentItem(
                    uri,
                    "logo",
                    1,
                    """
                    to square :size
                      forward :size
                    end

                    square 10
                    """.trimIndent(),
                ),
            ),
        )

        val result = service.semanticTokensFull(
            SemanticTokensParams(TextDocumentIdentifier(uri)),
        ).get()

        assertEquals(
            listOf(
                0, 0, 2, 0, 0,
                0, 3, 6, 1, 0,
                0, 7, 5, 3, 0,
                1, 2, 7, 2, 0,
                0, 8, 5, 3, 0,
                1, 0, 3, 0, 0,
                2, 0, 6, 1, 0,
                0, 7, 2, 5, 0,
            ),
            result.data,
        )
    }

    private fun recordingClient(published: MutableList<PublishDiagnosticsParams>): LanguageClient {
        return Proxy.newProxyInstance(
            LanguageClient::class.java.classLoader,
            arrayOf(LanguageClient::class.java),
        ) { _, method, args ->
            if (method.name == "publishDiagnostics") {
                published += args[0] as PublishDiagnosticsParams
            }
            defaultReturn(method.returnType)
        } as LanguageClient
    }

    private fun diagnosticMessage(diagnostic: Diagnostic): String {
        return diagnostic.message.left ?: diagnostic.message.right.value
    }

    private fun defaultReturn(returnType: Class<*>): Any? {
        return when {
            returnType == Void.TYPE -> null
            returnType == java.lang.Boolean.TYPE -> false
            returnType == java.lang.Integer.TYPE -> 0
            returnType == java.lang.Long.TYPE -> 0L
            returnType == java.lang.Double.TYPE -> 0.0
            returnType == java.lang.Float.TYPE -> 0.0f
            returnType == java.lang.Short.TYPE -> 0.toShort()
            returnType == java.lang.Byte.TYPE -> 0.toByte()
            CompletableFuture::class.java.isAssignableFrom(returnType) -> CompletableFuture.completedFuture(null)
            else -> null
        }
    }
}
