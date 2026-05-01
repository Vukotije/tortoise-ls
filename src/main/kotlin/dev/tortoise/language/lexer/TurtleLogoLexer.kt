package dev.tortoise.language.lexer

import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

class TurtleLogoLexer {
    private val keywordTypes = mapOf(
        "to" to LogoTokenType.KEYWORD_TO,
        "define" to LogoTokenType.KEYWORD_DEFINE,
        "end" to LogoTokenType.KEYWORD_END,
        "repeat" to LogoTokenType.KEYWORD_REPEAT,
        "for" to LogoTokenType.KEYWORD_FOR,
        "if" to LogoTokenType.KEYWORD_IF,
        "ifelse" to LogoTokenType.KEYWORD_IFELSE,
        "dotimes" to LogoTokenType.KEYWORD_DOTIMES,
        "while" to LogoTokenType.KEYWORD_WHILE,
        "until" to LogoTokenType.KEYWORD_UNTIL,
        "make" to LogoTokenType.KEYWORD_MAKE,
        "localmake" to LogoTokenType.KEYWORD_LOCALMAKE,
        "name" to LogoTokenType.KEYWORD_NAME,
    )

    fun tokenize(source: String): List<LogoToken> {
        val scanner = Scanner(source, keywordTypes)
        return scanner.scan()
    }

    private class Scanner(
        private val source: String,
        private val keywordTypes: Map<String, LogoTokenType>,
    ) {
        private val tokens = mutableListOf<LogoToken>()
        private var index = 0
        private var line = 1
        private var column = 1

        fun scan(): List<LogoToken> {
            while (!isAtEnd()) {
                when (val current = peek()) {
                    ' ', '\t' -> advance()
                    '\n', '\r' -> tokenizeNewline()
                    '[' -> addSingleCharacterToken(LogoTokenType.LEFT_BRACKET)
                    ']' -> addSingleCharacterToken(LogoTokenType.RIGHT_BRACKET)
                    '(' -> addSingleCharacterToken(LogoTokenType.LEFT_PAREN)
                    ')' -> addSingleCharacterToken(LogoTokenType.RIGHT_PAREN)
                    ';' -> skipComment()
                    '=' -> addSingleCharacterToken(LogoTokenType.EQUAL)
                    '<' -> addSingleCharacterToken(LogoTokenType.LESS_THAN)
                    '>' -> addSingleCharacterToken(LogoTokenType.GREATER_THAN)
                    '*' -> addSingleCharacterToken(LogoTokenType.STAR)
                    '"' -> tokenizeWordLiteral()
                    ':' -> tokenizeVariableReference()
                    '-' -> {
                        if (peekNext()?.isDigit() == true) {
                            tokenizeNumber()
                        } else {
                            tokenizeBadToken()
                        }
                    }
                    '+' -> {
                        if (peekNext()?.isDigit() == true) {
                            tokenizeNumber()
                        } else {
                            addSingleCharacterToken(LogoTokenType.PLUS)
                        }
                    }
                    else -> {
                        when {
                            current.isDigit() -> tokenizeNumber()
                            isIdentifierStart(current) -> tokenizeIdentifierOrKeyword()
                            else -> tokenizeBadToken()
                        }
                    }
                }
            }

            val eofPosition = currentPosition()
            tokens += LogoToken(
                type = LogoTokenType.EOF,
                lexeme = "",
                span = SourceSpan(eofPosition, eofPosition),
            )
            return tokens
        }

        private fun tokenizeIdentifierOrKeyword() {
            val startOffset = index
            val startPosition = currentPosition()
            advance()
            while (!isAtEnd() && isIdentifierPart(peek())) {
                advance()
            }

            val lexeme = source.substring(startOffset, index)
            val tokenType = keywordTypes[lexeme.lowercase()] ?: LogoTokenType.IDENTIFIER
            tokens += LogoToken(tokenType, lexeme, SourceSpan(startPosition, currentPosition()))
        }

        private fun tokenizeWordLiteral() {
            val startOffset = index
            val startPosition = currentPosition()
            advance()
            while (!isAtEnd() && isWordLiteralPart(peek())) {
                advance()
            }

            val lexeme = source.substring(startOffset, index)
            tokens += LogoToken(LogoTokenType.WORD_LITERAL, lexeme, SourceSpan(startPosition, currentPosition()))
        }

        private fun tokenizeVariableReference() {
            val startOffset = index
            val startPosition = currentPosition()
            advance()
            while (!isAtEnd() && isIdentifierPart(peek())) {
                advance()
            }

            val lexeme = source.substring(startOffset, index)
            val tokenType = if (lexeme.length == 1) LogoTokenType.BAD_TOKEN else LogoTokenType.VARIABLE_REFERENCE
            tokens += LogoToken(tokenType, lexeme, SourceSpan(startPosition, currentPosition()))
        }

        private fun tokenizeNumber() {
            val startOffset = index
            val startPosition = currentPosition()

            if (peek() == '+' || peek() == '-') {
                advance()
            }

            while (!isAtEnd() && peek().isDigit()) {
                advance()
            }

            if (!isAtEnd() && peek() == '.' && peekNext()?.isDigit() == true) {
                advance()
                while (!isAtEnd() && peek().isDigit()) {
                    advance()
                }
            }

            val lexeme = source.substring(startOffset, index)
            tokens += LogoToken(LogoTokenType.NUMBER, lexeme, SourceSpan(startPosition, currentPosition()))
        }

        private fun tokenizeNewline() {
            val startPosition = currentPosition()
            val startOffset = index
            if (peek() == '\r') {
                advance()
                if (!isAtEnd() && peek() == '\n') {
                    advance()
                } else {
                    line += 1
                    column = 1
                }
            } else {
                advance()
            }

            tokens += LogoToken(
                type = LogoTokenType.NEWLINE,
                lexeme = source.substring(startOffset, index),
                span = SourceSpan(startPosition, currentPosition()),
            )
        }

        private fun addSingleCharacterToken(type: LogoTokenType) {
            val startPosition = currentPosition()
            val startOffset = index
            advance()
            tokens += LogoToken(
                type = type,
                lexeme = source.substring(startOffset, index),
                span = SourceSpan(startPosition, currentPosition()),
            )
        }

        private fun skipComment() {
            while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
                advance()
            }
        }

        private fun tokenizeBadToken() {
            val startPosition = currentPosition()
            val startOffset = index
            advance()
            tokens += LogoToken(
                type = LogoTokenType.BAD_TOKEN,
                lexeme = source.substring(startOffset, index),
                span = SourceSpan(startPosition, currentPosition()),
            )
        }

        private fun peek(): Char = source[index]

        private fun peekNext(): Char? = source.getOrNull(index + 1)

        private fun isAtEnd(): Boolean = index >= source.length

        private fun currentPosition(): SourcePosition =
            SourcePosition(
                offset = index,
                line = line,
                column = column,
            )

        private fun advance(): Char {
            val char = source[index++]
            if (char == '\n') {
                line += 1
                column = 1
            } else {
                column += 1
            }
            return char
        }

        private fun isIdentifierStart(char: Char): Boolean = char.isLetter() || char == '_'

        private fun isIdentifierPart(char: Char): Boolean =
            char.isLetterOrDigit() || char == '_' || char == '?' || char == '.'

        private fun isWordLiteralPart(char: Char): Boolean =
            !char.isWhitespace() &&
                char != '[' &&
                char != ']' &&
                char != '(' &&
                char != ')'
    }
}
