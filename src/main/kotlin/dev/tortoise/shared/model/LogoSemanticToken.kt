package dev.tortoise.shared.model

import dev.tortoise.shared.text.SourceSpan

enum class LogoSemanticTokenType(val legendName: String) {
    KEYWORD("keyword"),
    PROCEDURE("procedure"),
    BUILT_IN("builtin"),
    PARAMETER("parameter"),
    VARIABLE("variable"),
    NUMBER("number"),
}

data class LogoSemanticToken(
    val type: LogoSemanticTokenType,
    val span: SourceSpan,
)

data class LogoSemanticTokenLegend(
    val tokenTypes: List<String>,
)

data class LogoSemanticTokens(
    val legend: LogoSemanticTokenLegend,
    val tokens: List<LogoSemanticToken>,
)
