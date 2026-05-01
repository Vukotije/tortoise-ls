package dev.tortoise.server.protocol

import dev.tortoise.shared.model.LogoSemanticTokenLegend
import dev.tortoise.shared.model.LogoSemanticTokenType
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.messages.Either

object ServerCapabilityFactory {
    fun minimalCapabilities(): ServerCapabilities =
        ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)
            definitionProvider = Either.forLeft(true)
            referencesProvider = Either.forLeft(true)
            completionProvider = CompletionOptions().apply {
                resolveProvider = false
            }
            semanticTokensProvider = SemanticTokensWithRegistrationOptions().apply {
                legend = SemanticTokensProtocolMapper.toLspLegend(SEMANTIC_TOKEN_LEGEND)
                full = Either.forLeft(true)
                range = Either.forLeft(false)
            }
        }

    val SEMANTIC_TOKEN_LEGEND = LogoSemanticTokenLegend(
        tokenTypes = LogoSemanticTokenType.entries.map { it.legendName },
    )
}
