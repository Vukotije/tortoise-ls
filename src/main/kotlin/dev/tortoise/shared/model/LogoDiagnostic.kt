package dev.tortoise.shared.model

import dev.tortoise.shared.text.SourceSpan

enum class LogoDiagnosticSeverity {
    ERROR,
    WARNING,
    INFORMATION,
    HINT,
}

data class LogoDiagnostic(
    val code: String,
    val message: String,
    val span: SourceSpan,
    val severity: LogoDiagnosticSeverity,
)
