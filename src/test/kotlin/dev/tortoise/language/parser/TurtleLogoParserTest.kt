package dev.tortoise.language.parser

import dev.tortoise.language.ast.LogoForStatement
import dev.tortoise.language.ast.LogoIfElseStatement
import dev.tortoise.language.ast.LogoProcedureDeclaration
import dev.tortoise.language.ast.LogoRepeatStatement
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
}
