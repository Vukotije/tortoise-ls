package dev.tortoise.language.diagnostics

import dev.tortoise.language.parser.TurtleLogoParser
import dev.tortoise.language.resolve.TurtleLogoResolver
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class BuiltInProcedureDiagnosticsTest {
    private val parser = TurtleLogoParser()
    private val resolver = TurtleLogoResolver()
    private val diagnosticsService = LogoDiagnosticsService()

    @Test
    fun `do not report unknown procedure for parseable expanded built-ins`() {
        val source = """
            to demo
              show "hello
              print "hello
              wait 10
              cleartext
              difference 5 3
              repcount
              thing "value
              def "demo
            end
        """.trimIndent()

        val parseResult = parser.parse(source)
        val resolutionResult = resolver.resolve(parseResult.program)
        val diagnostics = diagnosticsService.computeDiagnostics(parseResult, resolutionResult)

        assertFalse(
            diagnostics.any { diagnostic -> diagnostic.code == LogoDiagnosticCodes.UNKNOWN_PROCEDURE },
            "Expected expanded built-ins not to produce unknown-procedure diagnostics: $diagnostics",
        )
    }

    @Test
    fun `do not report unknown procedure for dotted loops comments and operator expressions`() {
        val source = """
            to demo
              make "a 1 ; initialize value
              do.while [
                print :a
                make "a :a + 1
              ] :a < 8
              test :a = 8 iftrue [show "done] iffalse [show "more]
            end
        """.trimIndent()

        val parseResult = parser.parse(source)
        val resolutionResult = resolver.resolve(parseResult.program)
        val diagnostics = diagnosticsService.computeDiagnostics(parseResult, resolutionResult)

        assertFalse(
            diagnostics.any { diagnostic -> diagnostic.code == LogoDiagnosticCodes.UNKNOWN_PROCEDURE },
            "Expected documented built-ins not to produce unknown-procedure diagnostics: $diagnostics",
        )
    }
}
