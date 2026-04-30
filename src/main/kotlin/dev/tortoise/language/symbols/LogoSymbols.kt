package dev.tortoise.language.symbols

import dev.tortoise.shared.text.SourceSpan

data class LogoProcedureSymbol(
    val name: String,
    val normalizedName: String,
    val declarationSpan: SourceSpan,
)

enum class LogoVariableSymbolKind {
    PARAMETER,
    MAKE_NAME,
    LOCALMAKE,
    LOOP,
}

data class LogoVariableSymbol(
    val name: String,
    val normalizedName: String,
    val kind: LogoVariableSymbolKind,
    val declarationSpan: SourceSpan,
    val scopeId: Int,
)
