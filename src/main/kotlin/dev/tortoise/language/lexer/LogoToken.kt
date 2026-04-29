package dev.tortoise.language.lexer

import dev.tortoise.shared.text.SourceSpan

data class LogoToken(
    val type: LogoTokenType,
    val lexeme: String,
    val span: SourceSpan,
)
