package dev.tortoise.server.protocol

import dev.tortoise.application.documents.DocumentStore
import dev.tortoise.application.documents.FullTextSyncStrategy
import dev.tortoise.application.documents.InMemoryDocumentStore
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService

class TortoiseLanguageServer(
    private val documentStore: DocumentStore = InMemoryDocumentStore(FullTextSyncStrategy()),
) : LanguageServer, LanguageClientAware {
    private val textDocumentService = TortoiseTextDocumentService(documentStore)
    private val workspaceService = TortoiseWorkspaceService()

    private var client: LanguageClient? = null
    private var shutdownRequested = false

    override fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> =
        CompletableFuture.completedFuture(
            InitializeResult(ServerCapabilityFactory.minimalCapabilities()),
        )

    override fun shutdown(): CompletableFuture<Any> {
        shutdownRequested = true
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {
        if (shutdownRequested) {
            return
        }
    }

    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    override fun getWorkspaceService(): WorkspaceService = workspaceService
}
