package dev.tortoise.language.diagnostics

import dev.tortoise.language.parser.LogoParseResult
import dev.tortoise.language.resolve.LogoResolutionResult
import dev.tortoise.shared.model.LogoDiagnostic

interface DiagnosticsService {
    fun computeDiagnostics(
        parseResult: LogoParseResult,
        resolutionResult: LogoResolutionResult,
    ): List<LogoDiagnostic>
}
