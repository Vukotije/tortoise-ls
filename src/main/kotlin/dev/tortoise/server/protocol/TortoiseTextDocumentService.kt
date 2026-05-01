package dev.tortoise.server.protocol

import dev.tortoise.application.analysis.AnalysisService
import dev.tortoise.application.documents.DocumentStore
import dev.tortoise.application.documents.TextDocumentChange
import dev.tortoise.application.features.CompletionService
import dev.tortoise.application.features.DefinitionService
import dev.tortoise.application.features.LogoCompletionService
import dev.tortoise.application.features.LogoDefinitionService
import dev.tortoise.application.features.LogoReferenceService
import dev.tortoise.application.features.ReferenceService
import dev.tortoise.shared.model.LogoCompletionItem
import dev.tortoise.shared.model.LogoCompletionItemKind
import dev.tortoise.shared.model.LogoDiagnostic
import dev.tortoise.shared.text.SourcePosition
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService

class TortoiseTextDocumentService(
    private val documentStore: DocumentStore,
    private val analysisService: AnalysisService,
    private val definitionService: DefinitionService = LogoDefinitionService(),
    private val referenceService: ReferenceService = LogoReferenceService(),
    private val completionService: CompletionService = LogoCompletionService(),
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

    override fun definition(
        params: DefinitionParams,
    ): CompletableFuture<Either<MutableList<out Location>, MutableList<out LocationLink>>> {
        val uri = params.textDocument.uri
        val document = documentStore.get(uri)
            ?: return CompletableFuture.completedFuture(Either.forLeft(mutableListOf()))
        val analysis = analysisService.getCached(uri)
            ?: return CompletableFuture.completedFuture(Either.forLeft(mutableListOf()))
        val position = document.text.toSourcePosition(
            line = params.position.line,
            character = params.position.character,
        )
        val targetSpan = definitionService.definition(analysis, position)
            ?: return CompletableFuture.completedFuture(Either.forLeft(mutableListOf()))

        return CompletableFuture.completedFuture(
            Either.forLeft(
                mutableListOf(
                    Location(
                        uri,
                        Range(
                            targetSpan.start.toLspPosition(),
                            targetSpan.end.toLspPosition(),
                        ),
                    ),
                ),
            ),
        )
    }

    override fun references(params: ReferenceParams): CompletableFuture<MutableList<out Location>> {
        val uri = params.textDocument.uri
        val document = documentStore.get(uri)
            ?: return CompletableFuture.completedFuture(mutableListOf())
        val analysis = analysisService.getCached(uri)
            ?: return CompletableFuture.completedFuture(mutableListOf())
        val position = document.text.toSourcePosition(
            line = params.position.line,
            character = params.position.character,
        )
        val includeDeclaration = params.context?.isIncludeDeclaration ?: false

        return CompletableFuture.completedFuture(
            referenceService.references(analysis, position, includeDeclaration)
                .map { span ->
                    Location(
                        uri,
                        Range(
                            span.start.toLspPosition(),
                            span.end.toLspPosition(),
                        ),
                    )
                }
                .toMutableList(),
        )
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        val uri = params.textDocument.uri
        val document = documentStore.get(uri)
            ?: return CompletableFuture.completedFuture(Either.forLeft(mutableListOf()))
        val analysis = analysisService.getCached(uri)
            ?: return CompletableFuture.completedFuture(Either.forLeft(mutableListOf()))
        val position = document.text.toSourcePosition(
            line = params.position.line,
            character = params.position.character,
        )

        return CompletableFuture.completedFuture(
            Either.forLeft(
                completionService.completions(analysis, position)
                    .map(::toLspCompletionItem)
                    .toMutableList(),
            ),
        )
    }

    private fun toLspCompletionItem(item: LogoCompletionItem): CompletionItem {
        return CompletionItem(item.label).apply {
            kind = when (item.kind) {
                LogoCompletionItemKind.BUILT_IN,
                LogoCompletionItemKind.PROCEDURE,
                -> CompletionItemKind.Function
                LogoCompletionItemKind.PARAMETER,
                LogoCompletionItemKind.VARIABLE,
                -> CompletionItemKind.Variable
            }
            sortText = item.sortText
        }
    }

    private fun String.toSourcePosition(line: Int, character: Int): SourcePosition {
        val targetLine = (line + 1).coerceAtLeast(1)
        val targetColumn = (character + 1).coerceAtLeast(1)
        var currentLine = 1
        var currentColumn = 1
        var offset = 0

        while (offset < length && (currentLine < targetLine || currentColumn < targetColumn)) {
            val char = this[offset]
            offset += 1
            if (char == '\r') {
                if (offset < length && this[offset] == '\n') {
                    offset += 1
                }
                currentLine += 1
                currentColumn = 1
            } else if (char == '\n') {
                currentLine += 1
                currentColumn = 1
            } else {
                currentColumn += 1
            }
        }

        return SourcePosition(offset = offset, line = targetLine, column = targetColumn)
    }

    private fun SourcePosition.toLspPosition(): org.eclipse.lsp4j.Position {
        return org.eclipse.lsp4j.Position(
            (line - 1).coerceAtLeast(0),
            (column - 1).coerceAtLeast(0),
        )
    }
}
