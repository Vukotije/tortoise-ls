package dev.tortoise.server.protocol

import dev.tortoise.application.analysis.AnalysisService
import dev.tortoise.application.documents.DocumentStore
import dev.tortoise.application.documents.TextDocumentChange
import dev.tortoise.shared.model.LogoDiagnostic
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.services.TextDocumentService

class TortoiseTextDocumentService(
    private val documentStore: DocumentStore,
    private val analysisService: AnalysisService,
    private val publishDiagnostics: (uri: String, version: Int?, diagnostics: List<LogoDiagnostic>) -> Unit,
) : TextDocumentService {
    override fun didOpen(params: DidOpenTextDocumentParams) {
        val document = params.textDocument
        val snapshot = documentStore.open(
            uri = document.uri,
            version = document.version,
            text = document.text,
        )
        val analysis = analysisService.analyze(snapshot)
        publishDiagnostics(snapshot.uri, snapshot.version, analysis.diagnostics)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val document = params.textDocument
        val snapshot = documentStore.change(
            uri = document.uri,
            version = document.version,
            changes = params.contentChanges.map { TextDocumentChange(it.text) },
        )
        val analysis = analysisService.analyze(snapshot)
        publishDiagnostics(snapshot.uri, snapshot.version, analysis.diagnostics)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val uri = params.textDocument.uri
        documentStore.close(uri)
        analysisService.clear(uri)
        publishDiagnostics(uri, null, emptyList())
    }

    override fun didSave(params: DidSaveTextDocumentParams) = Unit
}
