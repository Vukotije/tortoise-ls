package dev.tortoise.language.parser

import dev.tortoise.language.ast.LogoProgram
import dev.tortoise.shared.text.SourceSpan

data class LogoParseResult(
    val program: LogoProgram,
    val errors: List<LogoParseError>,
)

data class LogoParseError(
    val message: String,
    val span: SourceSpan,
)
