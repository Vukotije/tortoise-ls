package dev.tortoise.server.protocol

import dev.tortoise.application.documents.DocumentStore
import dev.tortoise.application.documents.TextDocumentChange
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.services.TextDocumentService

class TortoiseTextDocumentService(
    private val documentStore: DocumentStore,
) : TextDocumentService {
    override fun didOpen(params: DidOpenTextDocumentParams) {
        val document = params.textDocument
        documentStore.open(
            uri = document.uri,
            version = document.version,
            text = document.text,
        )
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val document = params.textDocument
        documentStore.change(
            uri = document.uri,
            version = document.version,
            changes = params.contentChanges.map { TextDocumentChange(it.text) },
        )
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        documentStore.close(params.textDocument.uri)
    }

    override fun didSave(params: DidSaveTextDocumentParams) = Unit
}
