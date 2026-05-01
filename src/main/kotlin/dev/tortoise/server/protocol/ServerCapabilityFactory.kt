package dev.tortoise.server.protocol

import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.messages.Either

object ServerCapabilityFactory {
    fun minimalCapabilities(): ServerCapabilities =
        ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)
            definitionProvider = Either.forLeft(true)
            referencesProvider = Either.forLeft(true)
        }
}
