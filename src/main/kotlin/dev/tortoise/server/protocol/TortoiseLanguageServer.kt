package dev.tortoise.server.protocol

import dev.tortoise.application.analysis.AnalysisService
import dev.tortoise.application.analysis.CachedAnalysisService
import dev.tortoise.application.documents.DocumentStore
import dev.tortoise.application.documents.FullTextSyncStrategy
import dev.tortoise.application.documents.InMemoryDocumentStore
import dev.tortoise.shared.model.LogoDiagnostic
import dev.tortoise.shared.model.LogoDiagnosticSeverity
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService

class TortoiseLanguageServer(
    private val documentStore: DocumentStore = InMemoryDocumentStore(FullTextSyncStrategy()),
    private val analysisService: AnalysisService = CachedAnalysisService(),
) : LanguageServer, LanguageClientAware {
    private val textDocumentService = TortoiseTextDocumentService(
        documentStore = documentStore,
        analysisService = analysisService,
        publishDiagnostics = ::publishDiagnostics,
    )
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

    private fun publishDiagnostics(uri: String, version: Int?, diagnostics: List<LogoDiagnostic>) {
        val client = client ?: return
        val params = PublishDiagnosticsParams(
            uri,
            diagnostics.map(::toLspDiagnostic),
        ).apply {
            this.version = version
        }
        client.publishDiagnostics(params)
    }

    private fun toLspDiagnostic(diagnostic: LogoDiagnostic): Diagnostic {
        return Diagnostic().apply {
            range = diagnostic.span.toLspRange()
            message = Either.forLeft<String, MarkupContent>(diagnostic.message)
            severity = toLspSeverity(diagnostic.severity)
            source = "tortoise-ls"
            code = Either.forLeft<String, Int>(diagnostic.code)
        }
    }

    private fun toLspSeverity(severity: LogoDiagnosticSeverity): DiagnosticSeverity {
        return when (severity) {
            LogoDiagnosticSeverity.ERROR -> DiagnosticSeverity.Error
            LogoDiagnosticSeverity.WARNING -> DiagnosticSeverity.Warning
            LogoDiagnosticSeverity.INFORMATION -> DiagnosticSeverity.Information
            LogoDiagnosticSeverity.HINT -> DiagnosticSeverity.Hint
        }
    }
}
