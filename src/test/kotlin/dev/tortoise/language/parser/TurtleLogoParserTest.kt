package dev.tortoise.language.parser

import dev.tortoise.language.ast.LogoForStatement
import dev.tortoise.language.ast.LogoBinaryExpression
import dev.tortoise.language.ast.LogoDoWhileStatement
import dev.tortoise.language.ast.LogoIfFalseStatement
import dev.tortoise.language.ast.LogoIfElseStatement
import dev.tortoise.language.ast.LogoProcedureDeclaration
import dev.tortoise.language.ast.LogoRepeatStatement
import dev.tortoise.language.ast.LogoTestStatement
import dev.tortoise.language.ast.LogoThingExpression
import dev.tortoise.language.ast.LogoVariableAssignmentStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TurtleLogoParserTest {
    private val parser = TurtleLogoParser()

    @Test
    fun `parse procedure declaration with repeat and ifelse`() {
        val source = """
            to square :size
            repeat 4 [
              make "x :size
              ifelse :x [
                localmake "x 1
              ] [
                name :x "x
              ]
            ]
            end
        """.trimIndent()

        val result = parser.parse(source)

        assertTrue(result.errors.isEmpty(), "Unexpected parser errors: ${result.errors}")
        assertEquals(1, result.program.statements.size)
        val procedure = assertInstanceOf(LogoProcedureDeclaration::class.java, result.program.statements.single())
        assertEquals("square", procedure.name)
        assertEquals(listOf("size"), procedure.parameters.map { it.name })
        assertEquals(1, procedure.body.statements.size)

        val repeat = assertInstanceOf(LogoRepeatStatement::class.java, procedure.body.statements[0])
        assertNotNull(repeat.block)
        val repeatBlock = repeat.block!!
        assertEquals(2, repeatBlock.statements.size)
        assertInstanceOf(LogoIfElseStatement::class.java, repeatBlock.statements[1])

        assertEquals(1, procedure.span.start.line)
        assertTrue(procedure.span.end.line >= procedure.span.start.line)
    }

    @Test
    fun `parse supported control structures at top level`() {
        val source = """
            for [i 1 10 2] [forward :i]
            dotimes [j 5] [right 90]
            while [:x] [make "x 1]
            until [:x] [make "x 0]
        """.trimIndent()

        val result = parser.parse(source)

        assertTrue(result.errors.isEmpty(), "Unexpected parser errors: ${result.errors}")
        assertEquals(4, result.program.statements.size)

        val forStatement = assertInstanceOf(LogoForStatement::class.java, result.program.statements[0])
        assertEquals("i", forStatement.header?.variableName)
        assertEquals(3, forStatement.header?.components?.size)
    }

    @Test
    fun `parse recovers from malformed procedure bodies`() {
        val source = """
            to broken :n
            repeat 2 [
              make "x :n
              if :x [make "y 1
            end
        """.trimIndent()

        val result = parser.parse(source)

        assertFalse(result.errors.isEmpty())
        assertEquals(1, result.program.statements.size)
        assertInstanceOf(LogoProcedureDeclaration::class.java, result.program.statements[0])
    }

    @Test
    fun `parse continues after bad tokens`() {
        val source = """
            @
            make "x 1
        """.trimIndent()

        val result = parser.parse(source)

        assertFalse(result.errors.isEmpty())
        assertEquals(2, result.program.statements.size)
        assertInstanceOf(LogoVariableAssignmentStatement::class.java, result.program.statements[1])
    }

    @Test
    fun `parse documented test branches and infix comparisons conservatively`() {
        val source = """
            test 3>4 iftrue [print "true] iffalse [print "false]
        """.trimIndent()

        val result = parser.parse(source)

        assertTrue(result.errors.isEmpty(), "Unexpected parser errors: ${result.errors}")
        assertEquals(3, result.program.statements.size)
        val test = assertInstanceOf(LogoTestStatement::class.java, result.program.statements[0])
        assertInstanceOf(LogoBinaryExpression::class.java, test.condition)
        assertInstanceOf(dev.tortoise.language.ast.LogoIfTrueStatement::class.java, result.program.statements[1])
        assertInstanceOf(LogoIfFalseStatement::class.java, result.program.statements[2])
    }

    @Test
    fun `parse dotted post-test loop and thing expression`() {
        val source = """
            do.while [
              make "a random 10
              show thing "a
            ] :a < 8
        """.trimIndent()

        val result = parser.parse(source)

        assertTrue(result.errors.isEmpty(), "Unexpected parser errors: ${result.errors}")
        val loop = assertInstanceOf(LogoDoWhileStatement::class.java, result.program.statements.single())
        assertEquals(2, loop.block?.statements?.size)
        assertInstanceOf(LogoBinaryExpression::class.java, loop.condition)
        val show = assertInstanceOf(
            dev.tortoise.language.ast.LogoCommandStatement::class.java,
            loop.block?.statements?.get(1),
        )
        assertInstanceOf(LogoThingExpression::class.java, show.arguments.single())
    }

    @Test
    fun `parse Turtle Academy define list procedure form`() {
        val source = """
            define "star [[n][repeat 5 [fd :n rt 144]]]
        """.trimIndent()

        val result = parser.parse(source)

        assertTrue(result.errors.isEmpty(), "Unexpected parser errors: ${result.errors}")
        val procedure = assertInstanceOf(LogoProcedureDeclaration::class.java, result.program.statements.single())
        assertEquals("star", procedure.name)
        assertEquals(listOf("n"), procedure.parameters.map { it.name })
        assertEquals(1, procedure.body.statements.size)
    }
}
