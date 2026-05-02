package dev.tortoise.language.diagnostics

import dev.tortoise.language.parser.LogoParseError
import dev.tortoise.language.parser.LogoParseResult
import dev.tortoise.language.resolve.LogoResolutionResult
import dev.tortoise.shared.model.LogoDiagnostic
import dev.tortoise.shared.model.LogoDiagnosticSeverity

class LogoDiagnosticsService : DiagnosticsService {
    override fun computeDiagnostics(
        parseResult: LogoParseResult,
        resolutionResult: LogoResolutionResult,
    ): List<LogoDiagnostic> {
        val diagnostics = mutableListOf<LogoDiagnostic>()

        diagnostics += resolutionResult.unresolvedProcedureReferences.map { unresolved ->
            LogoDiagnostic(
                code = LogoDiagnosticCodes.UNKNOWN_PROCEDURE,
                message = "Unknown procedure '${unresolved.name}'.",
                span = unresolved.span,
                severity = LogoDiagnosticSeverity.ERROR,
            )
        }

        diagnostics += resolutionResult.unresolvedVariableReferences.map { unresolved ->
            LogoDiagnostic(
                code = LogoDiagnosticCodes.UNKNOWN_VARIABLE,
                message = "Unknown variable ':${unresolved.name}'.",
                span = unresolved.span,
                severity = LogoDiagnosticSeverity.ERROR,
            )
        }

        diagnostics += resolutionResult.procedureTable.duplicateDeclarationsByName.values
            .flatten()
            .map { duplicate ->
                LogoDiagnostic(
                    code = LogoDiagnosticCodes.DUPLICATE_PROCEDURE_DECLARATION,
                    message = "Duplicate procedure declaration '${duplicate.name}'.",
                    span = duplicate.declarationSpan,
                    severity = LogoDiagnosticSeverity.ERROR,
                )
            }

        diagnostics += parseResult.errors
            .filter(::isMalformedDeclarationError)
            .map { error ->
                LogoDiagnostic(
                    code = LogoDiagnosticCodes.MALFORMED_DECLARATION_BLOCK,
                    message = error.message,
                    span = error.span,
                    severity = LogoDiagnosticSeverity.ERROR,
                )
            }

        return diagnostics.sortedWith(
            compareBy<LogoDiagnostic>(
                { it.span.start.offset },
                { it.span.end.offset },
                { it.code },
                { it.message },
            ),
        )
    }

    private fun isMalformedDeclarationError(error: LogoParseError): Boolean {
        val message = error.message
        return message.contains("Expected procedure name after") ||
            message.contains("Expected parameter like") ||
            message.contains("Expected 'end' before starting another procedure.") ||
            message.contains("Expected 'end' to close procedure declaration.")
    }
}
