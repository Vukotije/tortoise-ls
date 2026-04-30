package dev.tortoise.language.symbols

data class LogoProcedureSymbolTable(
    private val declarationsByName: Map<String, LogoProcedureSymbol>,
    val duplicateDeclarationsByName: Map<String, List<LogoProcedureSymbol>>,
) {
    fun resolve(name: String): LogoProcedureSymbol? = declarationsByName[name.lowercase()]

    fun allDeclarations(): Collection<LogoProcedureSymbol> = declarationsByName.values
}
