package dev.tortoise.language.lexer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TurtleLogoLexerTest {
    private val lexer = TurtleLogoLexer()

    @Test
    fun `tokenize recognizes core declaration and control keywords`() {
        val source = """
            TO square :size
            repeat 4 [forward :size right 90]
            end
        """.trimIndent()

        val tokenTypes = lexer.tokenize(source).map { it.type }

        assertEquals(
            listOf(
                LogoTokenType.KEYWORD_TO,
                LogoTokenType.IDENTIFIER,
                LogoTokenType.VARIABLE_REFERENCE,
                LogoTokenType.NEWLINE,
                LogoTokenType.KEYWORD_REPEAT,
                LogoTokenType.NUMBER,
                LogoTokenType.LEFT_BRACKET,
                LogoTokenType.IDENTIFIER,
                LogoTokenType.VARIABLE_REFERENCE,
                LogoTokenType.IDENTIFIER,
                LogoTokenType.NUMBER,
                LogoTokenType.RIGHT_BRACKET,
                LogoTokenType.NEWLINE,
                LogoTokenType.KEYWORD_END,
                LogoTokenType.EOF,
            ),
            tokenTypes,
        )
    }

    @Test
    fun `tokenize recognizes variable forms and signed decimals`() {
        val source = """
            localmake "size -10.5
            name +4 "count
            make "count :size
        """.trimIndent()

        val tokenTypes = lexer.tokenize(source).map { it.type }

        assertEquals(
            listOf(
                LogoTokenType.KEYWORD_LOCALMAKE,
                LogoTokenType.WORD_LITERAL,
                LogoTokenType.NUMBER,
                LogoTokenType.NEWLINE,
                LogoTokenType.KEYWORD_NAME,
                LogoTokenType.NUMBER,
                LogoTokenType.WORD_LITERAL,
                LogoTokenType.NEWLINE,
                LogoTokenType.KEYWORD_MAKE,
                LogoTokenType.WORD_LITERAL,
                LogoTokenType.VARIABLE_REFERENCE,
                LogoTokenType.EOF,
            ),
            tokenTypes,
        )
    }

    @Test
    fun `tokenize tracks source spans`() {
        val source = "to box\nend\n"
        val tokens = lexer.tokenize(source)

        assertEquals(1, tokens[0].span.start.line)
        assertEquals(1, tokens[0].span.start.column)
        assertEquals(1, tokens[0].span.end.line)
        assertEquals(3, tokens[0].span.end.column)

        assertEquals(1, tokens[2].span.start.line)
        assertEquals(7, tokens[2].span.start.column)
        assertEquals(2, tokens[2].span.end.line)
        assertEquals(1, tokens[2].span.end.column)
    }

    @Test
    fun `tokenize emits bad tokens for unsupported characters`() {
        val source = "to @proc\nend\n"
        val tokens = lexer.tokenize(source)

        val badToken = tokens.first { it.type == LogoTokenType.BAD_TOKEN }
        assertEquals("@", badToken.lexeme)
    }

    @Test
    fun `tokenize recognizes dotted identifiers operators and comments`() {
        val source = """
            do.while [ make "x :x + 1 ] :x < 8 ; keep looping
            if 2>1 [print "ok]
            if (random 2) = 0 [show "zero]
            dotimes [i 5] [show :i * :i]
        """.trimIndent()

        val tokens = lexer.tokenize(source)
        val tokenTypes = tokens.map { it.type }

        assertEquals("do.while", tokens.first().lexeme)
        assertTrue(LogoTokenType.PLUS in tokenTypes)
        assertTrue(LogoTokenType.LESS_THAN in tokenTypes)
        assertTrue(LogoTokenType.GREATER_THAN in tokenTypes)
        assertTrue(LogoTokenType.EQUAL in tokenTypes)
        assertTrue(LogoTokenType.STAR in tokenTypes)
        assertFalse(tokens.any { it.lexeme == ";" })
    }
}
