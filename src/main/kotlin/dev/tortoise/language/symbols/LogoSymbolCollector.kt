package dev.tortoise.language.symbols

import dev.tortoise.language.ast.LogoProcedureDeclaration
import dev.tortoise.language.ast.LogoProgram

data class LogoSymbolCollection(
    val procedureTable: LogoProcedureSymbolTable,
)

class LogoSymbolCollector {
    fun collect(program: LogoProgram): LogoSymbolCollection {
        val firstDeclarations = linkedMapOf<String, LogoProcedureSymbol>()
        val duplicates = linkedMapOf<String, MutableList<LogoProcedureSymbol>>()

        for (statement in program.statements) {
            if (statement !is LogoProcedureDeclaration) {
                continue
            }

            val name = statement.name ?: continue
            val normalizedName = name.lowercase()
            val symbol = LogoProcedureSymbol(
                name = name,
                normalizedName = normalizedName,
                declarationSpan = statement.span,
            )

            if (firstDeclarations.containsKey(normalizedName)) {
                duplicates.getOrPut(normalizedName) { mutableListOf() }.add(symbol)
            } else {
                firstDeclarations[normalizedName] = symbol
            }
        }

        return LogoSymbolCollection(
            procedureTable = LogoProcedureSymbolTable(
                declarationsByName = firstDeclarations.toMap(),
                duplicateDeclarationsByName = duplicates.mapValues { it.value.toList() },
            ),
        )
    }
}
