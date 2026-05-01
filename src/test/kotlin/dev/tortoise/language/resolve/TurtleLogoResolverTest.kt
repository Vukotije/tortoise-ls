package dev.tortoise.language.resolve

import dev.tortoise.language.parser.TurtleLogoParser
import dev.tortoise.language.symbols.LogoVariableSymbolKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TurtleLogoResolverTest {
    private val parser = TurtleLogoParser()
    private val resolver = TurtleLogoResolver()

    @Test
    fun `resolve procedure calls against single-file symbol table`() {
        val source = """
            to alpha
              beta
              forward 10
            end

            to beta
              forward 1
            end

            alpha
            missingProc
        """.trimIndent()

        val result = resolve(source)

        val declaredProcedureNames = result.procedureTable
            .allDeclarations()
            .map { it.normalizedName }
            .toSet()
        assertEquals(setOf("alpha", "beta"), declaredProcedureNames)

        val betaCall = result.procedureReferences.first { it.name.equals("beta", ignoreCase = true) }
        assertNotNull(betaCall.declaration)
        assertEquals("beta", betaCall.declaration?.normalizedName)

        val alphaCall = result.procedureReferences.first { it.name.equals("alpha", ignoreCase = true) }
        assertNotNull(alphaCall.declaration)
        assertEquals("alpha", alphaCall.declaration?.normalizedName)

        val builtInCall = result.procedureReferences.first { it.name.equals("forward", ignoreCase = true) }
        assertTrue(builtInCall.isBuiltIn)
        assertFalse(result.unresolvedProcedureReferences.any { it.name.equals("forward", ignoreCase = true) })

        assertEquals(1, result.unresolvedProcedureReferences.size)
        assertEquals("missingProc", result.unresolvedProcedureReferences.single().name)
    }

    @Test
    fun `resolve variable references across parameter local and global scopes`() {
        val source = """
            make "g 1
            to demo :size
              make "x :size
              name :x "y
              forward :x
              forward :y
              forward :g
            end
        """.trimIndent()

        val result = resolve(source)

        val parameter = result.variableDeclarations.first { it.name == "size" }
        val localX = result.variableDeclarations.first { it.name == "x" }
        val localY = result.variableDeclarations.first { it.name == "y" }
        val globalG = result.variableDeclarations.first { it.name == "g" }

        assertEquals(LogoVariableSymbolKind.PARAMETER, parameter.kind)
        assertEquals(LogoVariableSymbolKind.MAKE_NAME, localX.kind)
        assertEquals(LogoVariableSymbolKind.MAKE_NAME, localY.kind)
        assertEquals(LogoVariableSymbolKind.MAKE_NAME, globalG.kind)

        val sizeRefs = result.variableReferences.filter { it.name == "size" }
        val xRefs = result.variableReferences.filter { it.name == "x" }
        val yRefs = result.variableReferences.filter { it.name == "y" }
        val gRefs = result.variableReferences.filter { it.name == "g" }

        assertEquals(1, sizeRefs.size)
        assertEquals(parameter, sizeRefs.single().declaration)

        assertEquals(2, xRefs.size)
        assertTrue(xRefs.all { it.declaration == localX })

        assertEquals(1, yRefs.size)
        assertEquals(localY, yRefs.single().declaration)

        assertEquals(1, gRefs.size)
        assertEquals(globalG, gRefs.single().declaration)
    }

    @Test
    fun `resolve nearest visible symbol when names shadow outer scopes`() {
        val source = """
            make "x 0
            to demo :x
              forward :x
              localmake "x 1
              forward :x
              repeat 1 [
                localmake "x 2
                forward :x
              ]
            end
        """.trimIndent()

        val result = resolve(source)
        val xRefs = result.variableReferences.filter { it.name == "x" }

        assertEquals(3, xRefs.size)
        assertEquals(LogoVariableSymbolKind.PARAMETER, xRefs[0].declaration?.kind)
        assertEquals(LogoVariableSymbolKind.LOCALMAKE, xRefs[1].declaration?.kind)
        assertEquals(LogoVariableSymbolKind.LOCALMAKE, xRefs[2].declaration?.kind)
        assertNotEquals(xRefs[1].declaration, xRefs[2].declaration)
    }

    @Test
    fun `report unresolved variable and procedure references`() {
        val source = """
            to demo
              forward :missingValue
              missingProc
            end
        """.trimIndent()

        val result = resolve(source)

        assertEquals(1, result.unresolvedVariableReferences.size)
        assertEquals("missingValue", result.unresolvedVariableReferences.single().name)

        assertEquals(1, result.unresolvedProcedureReferences.size)
        assertEquals("missingProc", result.unresolvedProcedureReferences.single().name)

        assertTrue(result.procedureReferences.any { it.name.equals("forward", ignoreCase = true) && it.isBuiltIn })
    }

    @Test
    fun `resolve user procedure when its name collides with a built-in`() {
        val source = """
            to forward
              right 90
            end

            forward
        """.trimIndent()

        val result = resolve(source)
        val forwardCall = result.procedureReferences.single { it.name == "forward" }

        assertFalse(forwardCall.isBuiltIn)
        assertNotNull(forwardCall.declaration)
        assertEquals("forward", forwardCall.declaration?.normalizedName)
    }

    @Test
    fun `resolve thing reads and new control blocks through semantic core`() {
        val source = """
            make "a 1
            do.while [
              show thing "a
            ] :a < 8
            test :a = 1 iftrue [print :a] iffalse [print :missing]
        """.trimIndent()

        val result = resolve(source)

        assertTrue(result.procedureReferences.any { it.name == "do.while" && it.isBuiltIn })
        assertTrue(result.procedureReferences.any { it.name == "test" && it.isBuiltIn })
        assertTrue(result.procedureReferences.any { it.name == "iftrue" && it.isBuiltIn })
        assertTrue(result.procedureReferences.any { it.name == "iffalse" && it.isBuiltIn })
        assertTrue(result.variableReferences.any { it.name == "a" && it.span.start.line == 3 })
        assertTrue(result.unresolvedVariableReferences.any { it.name == "missing" })
    }

    private fun resolve(source: String): LogoResolutionResult {
        val parseResult = parser.parse(source)
        assertTrue(parseResult.errors.isEmpty(), "Unexpected parser errors: ${parseResult.errors}")
        return resolver.resolve(parseResult.program)
    }
}
