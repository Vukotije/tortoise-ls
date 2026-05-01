package dev.tortoise.language.ast

import dev.tortoise.shared.text.SourceSpan

sealed interface LogoNode {
    val span: SourceSpan
}

data class LogoProgram(
    val statements: List<LogoStatement>,
    override val span: SourceSpan,
) : LogoNode

sealed interface LogoStatement : LogoNode

data class LogoProcedureDeclaration(
    val keyword: String,
    val name: String?,
    val parameters: List<LogoParameter>,
    val body: LogoBlock,
    override val span: SourceSpan,
) : LogoStatement

data class LogoRepeatStatement(
    val count: LogoExpression?,
    val block: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoForStatement(
    val header: LogoForHeader?,
    val block: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoForHeader(
    val variableName: String?,
    val components: List<LogoExpression>,
    override val span: SourceSpan,
) : LogoNode

data class LogoIfStatement(
    val condition: LogoExpression?,
    val thenBlock: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoIfElseStatement(
    val condition: LogoExpression?,
    val thenBlock: LogoBlock?,
    val elseBlock: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoDotimesStatement(
    val header: LogoDotimesHeader?,
    val block: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoDotimesHeader(
    val variableName: String?,
    val count: LogoExpression?,
    override val span: SourceSpan,
) : LogoNode

data class LogoWhileStatement(
    val condition: LogoExpression?,
    val block: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoUntilStatement(
    val condition: LogoExpression?,
    val block: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoDoWhileStatement(
    val block: LogoBlock?,
    val condition: LogoExpression?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoDoUntilStatement(
    val block: LogoBlock?,
    val condition: LogoExpression?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoTestStatement(
    val condition: LogoExpression?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoIfTrueStatement(
    val block: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoIfFalseStatement(
    val block: LogoBlock?,
    override val span: SourceSpan,
) : LogoStatement

data class LogoVariableAssignmentStatement(
    val kind: AssignmentKind,
    val target: LogoWordExpression?,
    val value: LogoExpression?,
    val trailingArguments: List<LogoExpression>,
    override val span: SourceSpan,
) : LogoStatement {
    enum class AssignmentKind {
        MAKE,
        LOCALMAKE,
    }
}

data class LogoNameStatement(
    val value: LogoExpression?,
    val target: LogoWordExpression?,
    val trailingArguments: List<LogoExpression>,
    override val span: SourceSpan,
) : LogoStatement

data class LogoCommandStatement(
    val command: String,
    val arguments: List<LogoExpression>,
    override val span: SourceSpan,
) : LogoStatement

data class LogoBadStatement(
    val lexeme: String,
    override val span: SourceSpan,
) : LogoStatement

data class LogoBlock(
    val statements: List<LogoStatement>,
    override val span: SourceSpan,
) : LogoNode

data class LogoParameter(
    val name: String,
    override val span: SourceSpan,
) : LogoNode

sealed interface LogoExpression : LogoNode

data class LogoNumberExpression(
    val value: String,
    override val span: SourceSpan,
) : LogoExpression

data class LogoWordExpression(
    val value: String,
    override val span: SourceSpan,
) : LogoExpression

data class LogoVariableReferenceExpression(
    val name: String,
    override val span: SourceSpan,
) : LogoExpression

data class LogoThingExpression(
    val name: String,
    val target: LogoWordExpression,
    override val span: SourceSpan,
) : LogoExpression

data class LogoIdentifierExpression(
    val name: String,
    override val span: SourceSpan,
) : LogoExpression

data class LogoBinaryExpression(
    val left: LogoExpression,
    val operator: String,
    val right: LogoExpression,
    override val span: SourceSpan,
) : LogoExpression

data class LogoListExpression(
    val elements: List<LogoExpression>,
    override val span: SourceSpan,
) : LogoExpression

data class LogoParenthesizedExpression(
    val expressions: List<LogoExpression>,
    override val span: SourceSpan,
) : LogoExpression

data class LogoBadExpression(
    val lexeme: String,
    override val span: SourceSpan,
) : LogoExpression
