package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.language.lexer.LogoToken
import dev.tortoise.language.lexer.LogoTokenType
import dev.tortoise.language.symbols.LogoProcedureSymbol
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

internal fun DocumentAnalysis.tokenAt(position: SourcePosition): LogoToken? {
    return parseResult.tokens.firstOrNull { token -> token.span.contains(position) }
}

internal fun DocumentAnalysis.tokenAtStartOffset(offset: Int): LogoToken? {
    return parseResult.tokens.firstOrNull { token -> token.span.start.offset == offset }
}

internal fun DocumentAnalysis.procedureNameSpan(declaration: LogoProcedureSymbol): SourceSpan? {
    val tokens = parseResult.tokens
    return tokens.withIndex().firstOrNull { (index, token) ->
        token.isProcedureNameToken(declaration) &&
            token.span.isInside(declaration.declarationSpan) &&
            index > 0 &&
            tokens[index - 1].type.isProcedureDeclarationKeyword()
    }?.value?.span
}

private fun SourceSpan.contains(position: SourcePosition): Boolean {
    return position.offset >= start.offset && position.offset < end.offset
}

private fun SourceSpan.isInside(container: SourceSpan): Boolean {
    return start.offset >= container.start.offset && end.offset <= container.end.offset
}

private fun LogoTokenType.isProcedureDeclarationKeyword(): Boolean {
    return this == LogoTokenType.KEYWORD_TO || this == LogoTokenType.KEYWORD_DEFINE
}

private fun LogoToken.isProcedureNameToken(declaration: LogoProcedureSymbol): Boolean {
    return when (type) {
        LogoTokenType.IDENTIFIER -> lexeme.equals(declaration.name, ignoreCase = true)
        LogoTokenType.WORD_LITERAL -> lexeme.removePrefix("\"").equals(declaration.name, ignoreCase = true)
        else -> false
    }
}
