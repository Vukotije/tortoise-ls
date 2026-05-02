package dev.tortoise.shared.model

import dev.tortoise.shared.text.SourceSpan

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
    /** Source range replaced when the completion is applied; mapped to LSP `textEdit`. */
    val replaceSpan: SourceSpan,
)
