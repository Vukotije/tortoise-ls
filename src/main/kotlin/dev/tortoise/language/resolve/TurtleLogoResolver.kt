package dev.tortoise.language.resolve

import dev.tortoise.language.ast.LogoBlock
import dev.tortoise.language.ast.LogoBinaryExpression
import dev.tortoise.language.ast.LogoCommandStatement
import dev.tortoise.language.ast.LogoDotimesStatement
import dev.tortoise.language.ast.LogoDoUntilStatement
import dev.tortoise.language.ast.LogoDoWhileStatement
import dev.tortoise.language.ast.LogoExpression
import dev.tortoise.language.ast.LogoIfFalseStatement
import dev.tortoise.language.ast.LogoForStatement
import dev.tortoise.language.ast.LogoIdentifierExpression
import dev.tortoise.language.ast.LogoIfElseStatement
import dev.tortoise.language.ast.LogoIfStatement
import dev.tortoise.language.ast.LogoIfTrueStatement
import dev.tortoise.language.ast.LogoListExpression
import dev.tortoise.language.ast.LogoNameStatement
import dev.tortoise.language.ast.LogoParenthesizedExpression
import dev.tortoise.language.ast.LogoProcedureDeclaration
import dev.tortoise.language.ast.LogoProgram
import dev.tortoise.language.ast.LogoRepeatStatement
import dev.tortoise.language.ast.LogoStatement
import dev.tortoise.language.ast.LogoTestStatement
import dev.tortoise.language.ast.LogoThingExpression
import dev.tortoise.language.ast.LogoUntilStatement
import dev.tortoise.language.ast.LogoVariableAssignmentStatement
import dev.tortoise.language.ast.LogoVariableReferenceExpression
import dev.tortoise.language.ast.LogoWhileStatement
import dev.tortoise.language.ast.LogoWordExpression
import dev.tortoise.language.symbols.LogoProcedureSymbol
import dev.tortoise.language.symbols.LogoProcedureSymbolTable
import dev.tortoise.language.symbols.LogoSymbolCollector
import dev.tortoise.language.symbols.LogoVariableSymbol
import dev.tortoise.language.symbols.LogoVariableSymbolKind
import dev.tortoise.shared.text.SourceSpan

enum class LogoScopeKind {
    FILE,
    PROCEDURE,
    BLOCK,
}

data class LogoScopeSnapshot(
    val id: Int,
    val kind: LogoScopeKind,
    val parentId: Int?,
    val span: SourceSpan?,
    val declaredVariables: List<LogoVariableSymbol>,
)

data class LogoProcedureReferenceBinding(
    val name: String,
    val span: SourceSpan,
    val declaration: LogoProcedureSymbol?,
    val isBuiltIn: Boolean,
)

data class LogoVariableReferenceBinding(
    val name: String,
    val span: SourceSpan,
    val declaration: LogoVariableSymbol?,
)

data class LogoResolutionResult(
    val procedureTable: LogoProcedureSymbolTable,
    val scopes: List<LogoScopeSnapshot>,
    val variableDeclarations: List<LogoVariableSymbol>,
    val procedureReferences: List<LogoProcedureReferenceBinding>,
    val variableReferences: List<LogoVariableReferenceBinding>,
) {
    val unresolvedProcedureReferences: List<LogoProcedureReferenceBinding>
        get() = procedureReferences.filter { !it.isBuiltIn && it.declaration == null }

    val unresolvedVariableReferences: List<LogoVariableReferenceBinding>
        get() = variableReferences.filter { it.declaration == null }
}

class TurtleLogoResolver(
    private val symbolCollector: LogoSymbolCollector = LogoSymbolCollector(),
) {
    fun resolve(program: LogoProgram): LogoResolutionResult {
        val collection = symbolCollector.collect(program)
        val state = ResolverState(collection.procedureTable)
        state.resolveProgram(program)
        return state.toResult()
    }

    private class ResolverState(
        private val procedureTable: LogoProcedureSymbolTable,
    ) {
        private var nextScopeId: Int = 1
        private val scopes = mutableListOf<MutableScope>()
        private val variableDeclarations = mutableListOf<LogoVariableSymbol>()
        private val procedureReferences = mutableListOf<LogoProcedureReferenceBinding>()
        private val variableReferences = mutableListOf<LogoVariableReferenceBinding>()

        private val fileScope: MutableScope = createScope(kind = LogoScopeKind.FILE, parent = null)

        fun resolveProgram(program: LogoProgram) {
            fileScope.span = program.span
            for (statement in program.statements) {
                resolveStatement(statement, fileScope, procedureScope = null)
            }
        }

        fun toResult(): LogoResolutionResult {
            return LogoResolutionResult(
                procedureTable = procedureTable,
                scopes = scopes.map { scope ->
                    LogoScopeSnapshot(
                        id = scope.id,
                        kind = scope.kind,
                        parentId = scope.parent?.id,
                        span = scope.span,
                        declaredVariables = scope.declaredVariables.toList(),
                    )
                },
                variableDeclarations = variableDeclarations.toList(),
                procedureReferences = procedureReferences.toList(),
                variableReferences = variableReferences.toList(),
            )
        }

        private fun resolveStatement(
            statement: LogoStatement,
            currentScope: MutableScope,
            procedureScope: MutableScope?,
        ) {
            when (statement) {
                is LogoProcedureDeclaration -> resolveProcedureDeclaration(statement)
                is LogoRepeatStatement -> {
                    resolveExpression(statement.count, currentScope)
                    resolveBlock(statement.block, currentScope, procedureScope)
                }

                is LogoForStatement -> {
                    val header = statement.header
                    header?.components?.forEach { resolveExpression(it, currentScope) }
                    val loopScope = createScope(kind = LogoScopeKind.BLOCK, parent = currentScope)
                    loopScope.span = statement.block?.span ?: statement.span
                    header?.variableName?.let { variableName ->
                        declareVariable(
                            name = variableName,
                            kind = LogoVariableSymbolKind.LOOP,
                            scope = loopScope,
                            declarationSpan = header.span,
                        )
                    }
                    resolveBlock(statement.block, loopScope, procedureScope)
                }

                is LogoIfStatement -> {
                    resolveExpression(statement.condition, currentScope)
                    resolveBlock(statement.thenBlock, currentScope, procedureScope)
                }

                is LogoIfElseStatement -> {
                    resolveExpression(statement.condition, currentScope)
                    resolveBlock(statement.thenBlock, currentScope, procedureScope)
                    resolveBlock(statement.elseBlock, currentScope, procedureScope)
                }

                is LogoDotimesStatement -> {
                    val header = statement.header
                    resolveExpression(header?.count, currentScope)
                    val loopScope = createScope(kind = LogoScopeKind.BLOCK, parent = currentScope)
                    loopScope.span = statement.block?.span ?: statement.span
                    header?.variableName?.let { variableName ->
                        declareVariable(
                            name = variableName,
                            kind = LogoVariableSymbolKind.LOOP,
                            scope = loopScope,
                            declarationSpan = header.span,
                        )
                    }
                    resolveBlock(statement.block, loopScope, procedureScope)
                }

                is LogoWhileStatement -> {
                    resolveExpression(statement.condition, currentScope)
                    resolveBlock(statement.block, currentScope, procedureScope)
                }

                is LogoUntilStatement -> {
                    resolveExpression(statement.condition, currentScope)
                    resolveBlock(statement.block, currentScope, procedureScope)
                }

                is LogoDoWhileStatement -> {
                    resolveProcedureReference("do.while", statement.span)
                    resolveBlock(statement.block, currentScope, procedureScope)
                    resolveExpression(statement.condition, currentScope)
                }

                is LogoDoUntilStatement -> {
                    resolveProcedureReference("do.until", statement.span)
                    resolveBlock(statement.block, currentScope, procedureScope)
                    resolveExpression(statement.condition, currentScope)
                }

                is LogoTestStatement -> {
                    resolveProcedureReference("test", statement.span)
                    resolveExpression(statement.condition, currentScope)
                }

                is LogoIfTrueStatement -> {
                    resolveProcedureReference("iftrue", statement.span)
                    resolveBlock(statement.block, currentScope, procedureScope)
                }

                is LogoIfFalseStatement -> {
                    resolveProcedureReference("iffalse", statement.span)
                    resolveBlock(statement.block, currentScope, procedureScope)
                }

                is LogoVariableAssignmentStatement -> {
                    resolveExpression(statement.value, currentScope)
                    statement.trailingArguments.forEach { resolveExpression(it, currentScope) }
                    declareOrBindAssignmentTarget(
                        statement = statement,
                        currentScope = currentScope,
                        procedureScope = procedureScope,
                    )
                }

                is LogoNameStatement -> {
                    resolveExpression(statement.value, currentScope)
                    statement.trailingArguments.forEach { resolveExpression(it, currentScope) }
                    val target = statement.target ?: return
                    val targetName = target.value
                    if (lookupVisibleVariable(targetName, currentScope) == null) {
                        val declarationScope = procedureScope ?: fileScope
                        declareVariable(
                            name = targetName,
                            kind = LogoVariableSymbolKind.MAKE_NAME,
                            scope = declarationScope,
                            declarationSpan = target.span,
                        )
                    }
                }

                is LogoCommandStatement -> {
                    resolveProcedureReference(
                        name = statement.command,
                        span = statement.span,
                    )
                    resolveThingCommandRead(statement, currentScope)
                    statement.arguments.forEach { resolveExpression(it, currentScope) }
                }

                else -> Unit
            }
        }

        private fun resolveProcedureDeclaration(statement: LogoProcedureDeclaration) {
            val declarationScope = createScope(kind = LogoScopeKind.PROCEDURE, parent = fileScope)
            declarationScope.span = statement.span
            for (parameter in statement.parameters) {
                declareVariable(
                    name = parameter.name,
                    kind = LogoVariableSymbolKind.PARAMETER,
                    scope = declarationScope,
                    declarationSpan = parameter.span,
                )
            }
            for (bodyStatement in statement.body.statements) {
                resolveStatement(
                    statement = bodyStatement,
                    currentScope = declarationScope,
                    procedureScope = declarationScope,
                )
            }
        }

        private fun resolveBlock(
            block: LogoBlock?,
            parentScope: MutableScope,
            procedureScope: MutableScope?,
        ) {
            if (block == null) {
                return
            }
            val blockScope = createScope(kind = LogoScopeKind.BLOCK, parent = parentScope)
            blockScope.span = block.span
            for (statement in block.statements) {
                resolveStatement(
                    statement = statement,
                    currentScope = blockScope,
                    procedureScope = procedureScope,
                )
            }
        }

        private fun resolveExpression(
            expression: LogoExpression?,
            currentScope: MutableScope,
        ) {
            when (expression) {
                is LogoVariableReferenceExpression -> {
                    variableReferences += LogoVariableReferenceBinding(
                        name = expression.name,
                        span = expression.span,
                        declaration = lookupVisibleVariable(expression.name, currentScope),
                    )
                }

                is LogoThingExpression -> {
                    variableReferences += LogoVariableReferenceBinding(
                        name = expression.name,
                        span = expression.target.span,
                        declaration = lookupVisibleVariable(expression.name, currentScope),
                    )
                }

                is LogoBinaryExpression -> {
                    resolveExpression(expression.left, currentScope)
                    resolveExpression(expression.right, currentScope)
                }

                is LogoListExpression -> expression.elements.forEach { resolveExpression(it, currentScope) }
                is LogoParenthesizedExpression -> expression.expressions.forEach { resolveExpression(it, currentScope) }
                is LogoIdentifierExpression,
                null,
                -> Unit

                else -> Unit
            }
        }

        private fun resolveThingCommandRead(statement: LogoCommandStatement, currentScope: MutableScope) {
            if (!statement.command.equals("thing", ignoreCase = true)) {
                return
            }
            val target = statement.arguments.firstOrNull() as? LogoWordExpression ?: return
            variableReferences += LogoVariableReferenceBinding(
                name = target.value,
                span = target.span,
                declaration = lookupVisibleVariable(target.value, currentScope),
            )
        }

        private fun resolveProcedureReference(name: String, span: SourceSpan) {
            val declaration = procedureTable.resolve(name)
            val isBuiltIn = declaration == null && BuiltInProcedures.contains(name)
            procedureReferences += LogoProcedureReferenceBinding(
                name = name,
                span = span,
                declaration = declaration,
                isBuiltIn = isBuiltIn,
            )
        }

        private fun declareOrBindAssignmentTarget(
            statement: LogoVariableAssignmentStatement,
            currentScope: MutableScope,
            procedureScope: MutableScope?,
        ) {
            val target = statement.target ?: return
            val targetName = target.value
            if (statement.kind == LogoVariableAssignmentStatement.AssignmentKind.LOCALMAKE) {
                declareVariable(
                    name = targetName,
                    kind = LogoVariableSymbolKind.LOCALMAKE,
                    scope = currentScope,
                    declarationSpan = target.span,
                )
                return
            }

            if (lookupVisibleVariable(targetName, currentScope) == null) {
                // Explicit rule for `make` in this resolver:
                // if no visible declaration exists, declare in procedure scope when present, else file scope.
                val declarationScope = procedureScope ?: fileScope
                declareVariable(
                    name = targetName,
                    kind = LogoVariableSymbolKind.MAKE_NAME,
                    scope = declarationScope,
                    declarationSpan = target.span,
                )
            }
        }

        private fun declareVariable(
            name: String,
            kind: LogoVariableSymbolKind,
            scope: MutableScope,
            declarationSpan: SourceSpan,
        ): LogoVariableSymbol {
            val symbol = LogoVariableSymbol(
                name = name,
                normalizedName = name.lowercase(),
                kind = kind,
                declarationSpan = declarationSpan,
                scopeId = scope.id,
            )
            scope.latestByName[symbol.normalizedName] = symbol
            scope.declaredVariables += symbol
            variableDeclarations += symbol
            return symbol
        }

        private fun lookupVisibleVariable(name: String, currentScope: MutableScope): LogoVariableSymbol? {
            val normalizedName = name.lowercase()
            var scope: MutableScope? = currentScope
            while (scope != null) {
                val local = scope.latestByName[normalizedName]
                if (local != null) {
                    return local
                }
                scope = scope.parent
            }
            return null
        }

        private fun createScope(kind: LogoScopeKind, parent: MutableScope?): MutableScope {
            val id = nextScopeId++
            val scope = MutableScope(
                id = id,
                kind = kind,
                parent = parent,
            )
            scopes += scope
            return scope
        }

        private data class MutableScope(
            val id: Int,
            val kind: LogoScopeKind,
            val parent: MutableScope?,
            var span: SourceSpan? = null,
            val latestByName: MutableMap<String, LogoVariableSymbol> = mutableMapOf(),
            val declaredVariables: MutableList<LogoVariableSymbol> = mutableListOf(),
        )
    }
}
