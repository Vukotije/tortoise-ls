package dev.tortoise.shared.model

enum class LogoCompletionItemKind {
    BUILT_IN,
    PROCEDURE,
    PARAMETER,
    VARIABLE,
}

data class LogoCompletionItem(
    val label: String,
    val kind: LogoCompletionItemKind,
    val sortText: String,
)
