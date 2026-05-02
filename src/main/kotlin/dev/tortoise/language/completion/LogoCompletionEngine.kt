package dev.tortoise.language.completion

import dev.tortoise.language.parser.LogoParseResult
import dev.tortoise.language.resolve.BuiltInProcedures
import dev.tortoise.language.resolve.LogoResolutionResult
import dev.tortoise.language.resolve.LogoScopeKind
import dev.tortoise.language.resolve.LogoScopeSnapshot
import dev.tortoise.language.symbols.LogoVariableSymbol
import dev.tortoise.language.symbols.LogoVariableSymbolKind
import dev.tortoise.shared.model.LogoCompletionItem
import dev.tortoise.shared.model.LogoCompletionItemKind
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

class LogoCompletionEngine {
    fun complete(
        text: String,
        parseResult: LogoParseResult,
        resolutionResult: LogoResolutionResult,
        position: SourcePosition,
    ): List<LogoCompletionItem> {
        val prefix = completionPrefix(text, position)
        val depthByScope = resolutionResult.scopes.associate { scope -> scope.id to scope.depth(resolutionResult.scopes) }
        val visibleScopeIds = visibleScopeIds(resolutionResult.scopes, position)
        val visibleVariables = resolutionResult.variableDeclarations
            .filter { symbol -> symbol.scopeId in visibleScopeIds && symbol.declarationSpan.start.offset < position.offset }
            .nearestDeclarationsFirst(depthByScope)
        val userProcedureNames = resolutionResult.procedureTable
            .allDeclarations()
            .map { symbol -> symbol.normalizedName }
            .toSet()

        val items = mutableListOf<LogoCompletionItem>()
        items += BuiltInProcedures.names
            .filter { name -> name.lowercase() !in userProcedureNames }
            .map { name ->
                completionItem(name, LogoCompletionItemKind.BUILT_IN, category = 1, replaceSpan = prefix.replaceSpan)
            }
        items += resolutionResult.procedureTable.allDeclarations()
            .map { symbol ->
                completionItem(symbol.name, LogoCompletionItemKind.PROCEDURE, category = 2, replaceSpan = prefix.replaceSpan)
            }
        items += visibleVariables
            .filter { symbol -> symbol.kind == LogoVariableSymbolKind.PARAMETER }
            .map { symbol ->
                completionItem(":${symbol.name}", LogoCompletionItemKind.PARAMETER, category = 3, replaceSpan = prefix.replaceSpan)
            }
        items += visibleVariables
            .filter { symbol -> symbol.kind != LogoVariableSymbolKind.PARAMETER }
            .map { symbol ->
                completionItem(":${symbol.name}", LogoCompletionItemKind.VARIABLE, category = 4, replaceSpan = prefix.replaceSpan)
            }

        return items
            .filter { item -> item.label.startsWith(prefix.text, ignoreCase = true) }
            .distinctBy { item -> item.label.lowercase() }
            .sortedWith(compareBy({ it.sortText }, { it.label.lowercase() }))
    }

    private fun completionPrefix(text: String, position: SourcePosition): CompletionPrefix {
        val end = position.offset.coerceIn(0, text.length)
        var start = end
        while (start > 0 && !text[start - 1].isCompletionBoundary()) {
            start -= 1
        }
        val startPosition = text.positionAt(start)
        val endPosition = text.positionAt(end)
        return CompletionPrefix(
            text = text.substring(start, end),
            replaceSpan = SourceSpan(startPosition, endPosition),
        )
    }

    private fun Char.isCompletionBoundary(): Boolean {
        return isWhitespace() || this == '[' || this == ']' || this == '(' || this == ')'
    }

    private fun visibleScopeIds(scopes: List<LogoScopeSnapshot>, position: SourcePosition): Set<Int> {
        val scopesById = scopes.associateBy { scope -> scope.id }
        val deepestContainingScope = scopes
            .filter { scope -> scope.kind != LogoScopeKind.FILE && scope.span?.contains(position) == true }
            .maxWithOrNull(compareBy<LogoScopeSnapshot> { scope -> scope.depth(scopes) })

        val scopeIds = linkedSetOf<Int>()
        var scope = deepestContainingScope ?: scopes.firstOrNull { it.kind == LogoScopeKind.FILE }
        while (scope != null) {
            scopeIds += scope.id
            scope = scope.parentId?.let(scopesById::get)
        }
        return scopeIds
    }

    private fun List<LogoVariableSymbol>.nearestDeclarationsFirst(depthByScope: Map<Int, Int>): List<LogoVariableSymbol> {
        return sortedWith(
            compareByDescending<LogoVariableSymbol> { symbol -> depthByScope[symbol.scopeId] ?: 0 }
                .thenByDescending { symbol -> symbol.declarationSpan.start.offset },
        ).distinctBy { symbol -> symbol.normalizedName }
    }

    private fun completionItem(
        label: String,
        kind: LogoCompletionItemKind,
        category: Int,
        replaceSpan: SourceSpan,
    ): LogoCompletionItem {
        val normalizedLabel = label.lowercase()
        return LogoCompletionItem(
            label = label,
            kind = kind,
            sortText = "$category:$normalizedLabel",
            replaceSpan = replaceSpan,
        )
    }

    private fun String.positionAt(offset: Int): SourcePosition {
        val targetOffset = offset.coerceIn(0, length)
        var line = 1
        var column = 1
        var index = 0
        while (index < targetOffset) {
            val char = this[index]
            index += 1
            if (char == '\r') {
                if (index < targetOffset && this[index] == '\n') {
                    index += 1
                }
                line += 1
                column = 1
            } else if (char == '\n') {
                line += 1
                column = 1
            } else {
                column += 1
            }
        }
        return SourcePosition(offset = targetOffset, line = line, column = column)
    }

    private data class CompletionPrefix(
        val text: String,
        val replaceSpan: SourceSpan,
    )

    private fun LogoScopeSnapshot.depth(scopes: List<LogoScopeSnapshot>): Int {
        val scopesById = scopes.associateBy { scope -> scope.id }
        var depth = 0
        var parent = parentId?.let(scopesById::get)
        while (parent != null) {
            depth += 1
            parent = parent.parentId?.let(scopesById::get)
        }
        return depth
    }

    private fun SourceSpan.contains(position: SourcePosition): Boolean {
        return position.offset >= start.offset && position.offset <= end.offset
    }
}
