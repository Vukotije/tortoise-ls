package dev.tortoise.application.analysis

import dev.tortoise.language.parser.LogoParseResult
import dev.tortoise.language.resolve.LogoResolutionResult
import dev.tortoise.shared.model.LogoDiagnostic

data class DocumentAnalysis(
    val uri: String,
    val version: Int,
    val text: String,
    val parseResult: LogoParseResult,
    val resolutionResult: LogoResolutionResult,
    val diagnostics: List<LogoDiagnostic>,
)
