package dev.tortoise.language.parser

import dev.tortoise.language.ast.LogoBadExpression
import dev.tortoise.language.ast.LogoBadStatement
import dev.tortoise.language.ast.LogoBinaryExpression
import dev.tortoise.language.ast.LogoBlock
import dev.tortoise.language.ast.LogoCommandStatement
import dev.tortoise.language.ast.LogoDotimesHeader
import dev.tortoise.language.ast.LogoDotimesStatement
import dev.tortoise.language.ast.LogoDoUntilStatement
import dev.tortoise.language.ast.LogoDoWhileStatement
import dev.tortoise.language.ast.LogoExpression
import dev.tortoise.language.ast.LogoForHeader
import dev.tortoise.language.ast.LogoForStatement
import dev.tortoise.language.ast.LogoIfFalseStatement
import dev.tortoise.language.ast.LogoIdentifierExpression
import dev.tortoise.language.ast.LogoIfElseStatement
import dev.tortoise.language.ast.LogoIfStatement
import dev.tortoise.language.ast.LogoIfTrueStatement
import dev.tortoise.language.ast.LogoListExpression
import dev.tortoise.language.ast.LogoNameStatement
import dev.tortoise.language.ast.LogoNumberExpression
import dev.tortoise.language.ast.LogoParameter
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
import dev.tortoise.language.lexer.LogoToken
import dev.tortoise.language.lexer.LogoTokenType
import dev.tortoise.language.lexer.TurtleLogoLexer
import dev.tortoise.shared.text.SourceSpan

class TurtleLogoParser(
    private val lexer: TurtleLogoLexer = TurtleLogoLexer(),
) {
    fun parse(source: String): LogoParseResult = parseTokens(lexer.tokenize(source))

    fun parseTokens(tokens: List<LogoToken>): LogoParseResult {
        return ParserState(tokens).parseProgram()
    }

    private class ParserState(
        private val tokens: List<LogoToken>,
    ) {
        private val errors = mutableListOf<LogoParseError>()
        private val keywordExpressionTypes = setOf(
            LogoTokenType.KEYWORD_TO,
            LogoTokenType.KEYWORD_DEFINE,
            LogoTokenType.KEYWORD_END,
            LogoTokenType.KEYWORD_REPEAT,
            LogoTokenType.KEYWORD_FOR,
            LogoTokenType.KEYWORD_IF,
            LogoTokenType.KEYWORD_IFELSE,
            LogoTokenType.KEYWORD_DOTIMES,
            LogoTokenType.KEYWORD_WHILE,
            LogoTokenType.KEYWORD_UNTIL,
            LogoTokenType.KEYWORD_MAKE,
            LogoTokenType.KEYWORD_LOCALMAKE,
            LogoTokenType.KEYWORD_NAME,
        )

        private var current = 0

        fun parseProgram(): LogoParseResult {
            val statements = mutableListOf<LogoStatement>()
            skipNewlines()
            while (!isAtEnd()) {
                val before = current
                val statement = parseStatement(stopTokens = emptySet())
                if (statement != null) {
                    statements += statement
                }

                if (current == before) {
                    reportError("Parser did not advance at token '${peek().lexeme}'.", peek().span)
                    advance()
                }
                skipNewlines()
            }

            val programSpan = if (statements.isEmpty()) {
                peek().span
            } else {
                SourceSpan(statements.first().span.start, statements.last().span.end)
            }
            return LogoParseResult(
                program = LogoProgram(statements, programSpan),
                tokens = tokens,
                errors = errors.toList(),
            )
        }

        private fun parseStatement(stopTokens: Set<LogoTokenType>): LogoStatement? {
            if (isAtEnd() || peek().type in stopTokens) {
                return null
            }

            return when (peek().type) {
                LogoTokenType.KEYWORD_TO -> parseProcedureDeclaration()
                LogoTokenType.KEYWORD_DEFINE -> parseDefineStatement()

                LogoTokenType.KEYWORD_REPEAT -> parseRepeatStatement()
                LogoTokenType.KEYWORD_FOR -> parseForStatement()
                LogoTokenType.KEYWORD_IF -> parseIfStatement()
                LogoTokenType.KEYWORD_IFELSE -> parseIfElseStatement()
                LogoTokenType.KEYWORD_DOTIMES -> parseDotimesStatement()
                LogoTokenType.KEYWORD_WHILE -> parseWhileStatement()
                LogoTokenType.KEYWORD_UNTIL -> parseUntilStatement()
                LogoTokenType.KEYWORD_MAKE,
                LogoTokenType.KEYWORD_LOCALMAKE,
                -> parseVariableAssignmentStatement(stopTokens)

                LogoTokenType.KEYWORD_NAME -> parseNameStatement(stopTokens)
                LogoTokenType.IDENTIFIER -> parseIdentifierStatement(stopTokens)
                LogoTokenType.NEWLINE -> {
                    advance()
                    null
                }

                else -> {
                    val token = advance()
                    reportError("Unexpected token '${token.lexeme}' at statement start.", token.span)
                    LogoBadStatement(lexeme = token.lexeme, span = token.span)
                }
            }
        }

        private fun parseProcedureDeclaration(): LogoProcedureDeclaration {
            val keywordToken = advance()
            val nameToken = if (match(LogoTokenType.IDENTIFIER)) {
                previous()
            } else {
                reportError("Expected procedure name after '${keywordToken.lexeme}'.", peek().span)
                null
            }

            val parameters = mutableListOf<LogoParameter>()
            while (!isAtEnd() && !check(LogoTokenType.NEWLINE) && !check(LogoTokenType.KEYWORD_END)) {
                if (match(LogoTokenType.VARIABLE_REFERENCE)) {
                    val token = previous()
                    parameters += LogoParameter(
                        name = token.lexeme.removePrefix(":"),
                        span = token.span,
                    )
                    continue
                }

                val token = advance()
                reportError("Expected parameter like ':size' in procedure header.", token.span)
            }

            match(LogoTokenType.NEWLINE)

            val bodyStatements = mutableListOf<LogoStatement>()
            skipNewlines()
            while (!isAtEnd() && !check(LogoTokenType.KEYWORD_END)) {
                if (check(LogoTokenType.KEYWORD_TO) || check(LogoTokenType.KEYWORD_DEFINE)) {
                    reportError("Expected 'end' before starting another procedure.", peek().span)
                    break
                }

                val before = current
                val statement = parseStatement(stopTokens = setOf(LogoTokenType.KEYWORD_END))
                if (statement != null) {
                    bodyStatements += statement
                }
                if (current == before) {
                    reportError("Unable to parse procedure body statement.", peek().span)
                    advance()
                }
                skipNewlines()
            }

            val endToken = if (match(LogoTokenType.KEYWORD_END)) {
                previous()
            } else {
                reportError("Expected 'end' to close procedure declaration.", peek().span)
                null
            }

            val bodySpan = if (bodyStatements.isEmpty()) {
                val anchor = nameToken?.span?.end ?: keywordToken.span.end
                SourceSpan(anchor, anchor)
            } else {
                SourceSpan(bodyStatements.first().span.start, bodyStatements.last().span.end)
            }
            val body = LogoBlock(bodyStatements, bodySpan)
            val endPosition = endToken?.span?.end ?: body.span.end
            return LogoProcedureDeclaration(
                keyword = keywordToken.lexeme.lowercase(),
                name = nameToken?.lexeme,
                parameters = parameters,
                body = body,
                span = SourceSpan(keywordToken.span.start, endPosition),
            )
        }

        private fun parseDefineStatement(): LogoProcedureDeclaration {
            return if (peekNext()?.type == LogoTokenType.WORD_LITERAL) {
                parseDefineListProcedure()
            } else {
                parseProcedureDeclaration()
            }
        }

        private fun parseDefineListProcedure(): LogoProcedureDeclaration {
            val keywordToken = advance()
            val nameToken = if (match(LogoTokenType.WORD_LITERAL)) {
                previous()
            } else {
                reportError("Expected quoted procedure name after 'define'.", peek().span)
                null
            }
            val name = nameToken?.lexeme?.removePrefix("\"")

            val outerOpening = if (match(LogoTokenType.LEFT_BRACKET)) {
                previous()
            } else {
                reportError("Expected define body list '[[inputs][body]]'.", peek().span)
                null
            }

            val parameters = parseDefineParameterList()
            val body = if (outerOpening != null) {
                parseBlockOrError("Expected define procedure body block '[ ... ]'.")
            } else {
                null
            }

            val outerClosing = if (outerOpening != null && match(LogoTokenType.RIGHT_BRACKET)) {
                previous()
            } else {
                reportError("Expected closing ']' for define body list.", peek().span)
                null
            }

            val bodyBlock = body ?: LogoBlock(emptyList(), SourceSpan(nameToken?.span?.end ?: keywordToken.span.end, nameToken?.span?.end ?: keywordToken.span.end))
            val endPosition = outerClosing?.span?.end ?: bodyBlock.span.end
            return LogoProcedureDeclaration(
                keyword = keywordToken.lexeme.lowercase(),
                name = name,
                parameters = parameters,
                body = bodyBlock,
                span = SourceSpan(keywordToken.span.start, endPosition),
            )
        }

        private fun parseDefineParameterList(): List<LogoParameter> {
            if (!match(LogoTokenType.LEFT_BRACKET)) {
                reportError("Expected define input list '[inputs]'.", peek().span)
                return emptyList()
            }

            val parameters = mutableListOf<LogoParameter>()
            while (!isAtEnd() && !check(LogoTokenType.RIGHT_BRACKET)) {
                if (match(LogoTokenType.NEWLINE)) {
                    continue
                }
                val token = when {
                    match(LogoTokenType.IDENTIFIER) -> previous()
                    match(LogoTokenType.VARIABLE_REFERENCE) -> previous()
                    match(LogoTokenType.WORD_LITERAL) -> previous()
                    else -> {
                        val badToken = advance()
                        reportError("Expected input name in define input list.", badToken.span)
                        null
                    }
                }
                if (token != null) {
                    parameters += LogoParameter(
                        name = token.lexeme.removePrefix(":").removePrefix("\""),
                        span = token.span,
                    )
                }
            }

            if (!match(LogoTokenType.RIGHT_BRACKET)) {
                reportError("Expected closing ']' for define input list.", peek().span)
            }
            return parameters
        }

        private fun parseRepeatStatement(): LogoRepeatStatement {
            val keyword = advance()
            val count = parseExpressionOrError("Expected repeat count expression.")
            val block = parseBlockOrError("Expected repeat body block '[ ... ]'.")
            return LogoRepeatStatement(
                count = count,
                block = block,
                span = spanFrom(keyword.span, block?.span ?: count?.span),
            )
        }

        private fun parseForStatement(): LogoForStatement {
            val keyword = advance()
            val header = parseForHeader()
            val block = parseBlockOrError("Expected for body block '[ ... ]'.")
            return LogoForStatement(
                header = header,
                block = block,
                span = spanFrom(keyword.span, block?.span ?: header?.span),
            )
        }

        private fun parseForHeader(): LogoForHeader? {
            if (!match(LogoTokenType.LEFT_BRACKET)) {
                reportError("Expected for header list '[var start limit step]'.", peek().span)
                return null
            }
            val opening = previous()
            val variableName = when {
                match(LogoTokenType.IDENTIFIER) -> previous().lexeme
                match(LogoTokenType.VARIABLE_REFERENCE) -> previous().lexeme.removePrefix(":")
                match(LogoTokenType.WORD_LITERAL) -> previous().lexeme.removePrefix("\"")
                else -> {
                    reportError("Expected loop variable in for header.", peek().span)
                    null
                }
            }

            val components = mutableListOf<LogoExpression>()
            while (!isAtEnd() && !check(LogoTokenType.RIGHT_BRACKET)) {
                if (match(LogoTokenType.NEWLINE)) {
                    continue
                }
                val expression = parseExpression()
                if (expression != null) {
                    components += expression
                } else {
                    val token = advance()
                    reportError("Expected expression in for header.", token.span)
                }
            }

            val closing = if (match(LogoTokenType.RIGHT_BRACKET)) {
                previous()
            } else {
                reportError("Expected closing ']' for for header.", peek().span)
                null
            }

            if (components.size > 3) {
                reportError("For header accepts at most three expressions after loop variable.", components[3].span)
            }

            return LogoForHeader(
                variableName = variableName,
                components = components,
                span = spanFrom(opening.span, closing?.span),
            )
        }

        private fun parseIfStatement(): LogoIfStatement {
            val keyword = advance()
            val condition = parseExpressionOrError("Expected if condition expression.")
            val thenBlock = parseBlockOrError("Expected if body block '[ ... ]'.")
            return LogoIfStatement(
                condition = condition,
                thenBlock = thenBlock,
                span = spanFrom(keyword.span, thenBlock?.span ?: condition?.span),
            )
        }

        private fun parseIfElseStatement(): LogoIfElseStatement {
            val keyword = advance()
            val condition = parseExpressionOrError("Expected ifelse condition expression.")
            val thenBlock = parseBlockOrError("Expected ifelse then block '[ ... ]'.")
            val elseBlock = parseBlockOrError("Expected ifelse else block '[ ... ]'.")
            return LogoIfElseStatement(
                condition = condition,
                thenBlock = thenBlock,
                elseBlock = elseBlock,
                span = spanFrom(keyword.span, elseBlock?.span ?: thenBlock?.span ?: condition?.span),
            )
        }

        private fun parseDotimesStatement(): LogoDotimesStatement {
            val keyword = advance()
            val header = parseDotimesHeader()
            val block = parseBlockOrError("Expected dotimes body block '[ ... ]'.")
            return LogoDotimesStatement(
                header = header,
                block = block,
                span = spanFrom(keyword.span, block?.span ?: header?.span),
            )
        }

        private fun parseDotimesHeader(): LogoDotimesHeader? {
            if (!match(LogoTokenType.LEFT_BRACKET)) {
                reportError("Expected dotimes header list '[var count]'.", peek().span)
                return null
            }
            val opening = previous()

            val variableName = when {
                match(LogoTokenType.IDENTIFIER) -> previous().lexeme
                match(LogoTokenType.VARIABLE_REFERENCE) -> previous().lexeme.removePrefix(":")
                match(LogoTokenType.WORD_LITERAL) -> previous().lexeme.removePrefix("\"")
                else -> {
                    reportError("Expected loop variable in dotimes header.", peek().span)
                    null
                }
            }

            val count = if (!isAtEnd() && !check(LogoTokenType.RIGHT_BRACKET)) {
                parseExpression()
            } else {
                null
            }
            if (count == null) {
                reportError("Expected count expression in dotimes header.", peek().span)
            }

            val extraComponents = mutableListOf<LogoExpression>()
            while (!isAtEnd() && !check(LogoTokenType.RIGHT_BRACKET)) {
                if (match(LogoTokenType.NEWLINE)) {
                    continue
                }
                val expression = parseExpression()
                if (expression != null) {
                    extraComponents += expression
                } else {
                    val token = advance()
                    reportError("Unexpected token in dotimes header.", token.span)
                }
            }
            if (extraComponents.isNotEmpty()) {
                reportError("Dotimes header accepts only one count expression.", extraComponents.first().span)
            }

            val closing = if (match(LogoTokenType.RIGHT_BRACKET)) {
                previous()
            } else {
                reportError("Expected closing ']' for dotimes header.", peek().span)
                null
            }

            return LogoDotimesHeader(
                variableName = variableName,
                count = count,
                span = spanFrom(opening.span, closing?.span),
            )
        }

        private fun parseWhileStatement(): LogoWhileStatement {
            val keyword = advance()
            val condition = parseExpressionOrError("Expected while condition expression.")
            val block = parseBlockOrError("Expected while body block '[ ... ]'.")
            return LogoWhileStatement(
                condition = condition,
                block = block,
                span = spanFrom(keyword.span, block?.span ?: condition?.span),
            )
        }

        private fun parseUntilStatement(): LogoUntilStatement {
            val keyword = advance()
            val condition = parseExpressionOrError("Expected until condition expression.")
            val block = parseBlockOrError("Expected until body block '[ ... ]'.")
            return LogoUntilStatement(
                condition = condition,
                block = block,
                span = spanFrom(keyword.span, block?.span ?: condition?.span),
            )
        }

        private fun parseVariableAssignmentStatement(stopTokens: Set<LogoTokenType>): LogoVariableAssignmentStatement {
            val keyword = advance()
            val kind = if (keyword.type == LogoTokenType.KEYWORD_LOCALMAKE) {
                LogoVariableAssignmentStatement.AssignmentKind.LOCALMAKE
            } else {
                LogoVariableAssignmentStatement.AssignmentKind.MAKE
            }

            val target = if (match(LogoTokenType.WORD_LITERAL)) {
                LogoWordExpression(previous().lexeme.removePrefix("\""), previous().span)
            } else {
                reportError("Expected word literal target after '${keyword.lexeme}'.", peek().span)
                null
            }

            val value = parseExpressionIfPresent(stopTokens)
            if (value == null) {
                reportError("Expected value expression after '${keyword.lexeme}'.", peek().span)
            }

            val trailing = parseTrailingArguments(stopTokens)
            return LogoVariableAssignmentStatement(
                kind = kind,
                target = target,
                value = value,
                trailingArguments = trailing,
                span = spanFrom(keyword.span, trailing.lastOrNull()?.span ?: value?.span ?: target?.span),
            )
        }

        private fun parseNameStatement(stopTokens: Set<LogoTokenType>): LogoNameStatement {
            val keyword = advance()
            val value = parseExpressionIfPresent(stopTokens)
            if (value == null) {
                reportError("Expected value expression after 'name'.", peek().span)
            }

            val target = if (match(LogoTokenType.WORD_LITERAL)) {
                LogoWordExpression(previous().lexeme.removePrefix("\""), previous().span)
            } else {
                reportError("Expected target word literal for name statement.", peek().span)
                null
            }

            val trailing = parseTrailingArguments(stopTokens)
            return LogoNameStatement(
                value = value,
                target = target,
                trailingArguments = trailing,
                span = spanFrom(keyword.span, trailing.lastOrNull()?.span ?: target?.span ?: value?.span),
            )
        }

        private fun parseIdentifierStatement(stopTokens: Set<LogoTokenType>): LogoStatement {
            return when (peek().lexeme.lowercase()) {
                "do.while" -> parseDoWhileStatement()
                "do.until" -> parseDoUntilStatement()
                "test" -> parseTestStatement()
                "iftrue" -> parseIfTrueStatement()
                "iffalse" -> parseIfFalseStatement()
                else -> parseCommandStatement(stopTokens)
            }
        }

        private fun parseDoWhileStatement(): LogoDoWhileStatement {
            val keyword = advance()
            val block = parseBlockOrError("Expected do.while body block '[ ... ]'.")
            val condition = parseExpressionOrError("Expected do.while condition expression.")
            return LogoDoWhileStatement(
                block = block,
                condition = condition,
                span = spanFrom(keyword.span, condition?.span ?: block?.span),
            )
        }

        private fun parseDoUntilStatement(): LogoDoUntilStatement {
            val keyword = advance()
            val block = parseBlockOrError("Expected do.until body block '[ ... ]'.")
            val condition = parseExpressionOrError("Expected do.until condition expression.")
            return LogoDoUntilStatement(
                block = block,
                condition = condition,
                span = spanFrom(keyword.span, condition?.span ?: block?.span),
            )
        }

        private fun parseTestStatement(): LogoTestStatement {
            val keyword = advance()
            val condition = parseExpressionOrError("Expected test condition expression.")
            return LogoTestStatement(
                condition = condition,
                span = spanFrom(keyword.span, condition?.span),
            )
        }

        private fun parseIfTrueStatement(): LogoIfTrueStatement {
            val keyword = advance()
            val block = parseBlockOrError("Expected iftrue body block '[ ... ]'.")
            return LogoIfTrueStatement(
                block = block,
                span = spanFrom(keyword.span, block?.span),
            )
        }

        private fun parseIfFalseStatement(): LogoIfFalseStatement {
            val keyword = advance()
            val block = parseBlockOrError("Expected iffalse body block '[ ... ]'.")
            return LogoIfFalseStatement(
                block = block,
                span = spanFrom(keyword.span, block?.span),
            )
        }

        private fun parseCommandStatement(stopTokens: Set<LogoTokenType>): LogoCommandStatement {
            val commandToken = advance()
            val arguments = mutableListOf<LogoExpression>()
            while (!isStatementBoundary(stopTokens)) {
                val expression = parseExpression()
                if (expression != null) {
                    arguments += expression
                    continue
                }

                val token = advance()
                reportError("Expected command argument expression.", token.span)
            }

            return LogoCommandStatement(
                command = commandToken.lexeme,
                arguments = arguments,
                span = spanFrom(commandToken.span, arguments.lastOrNull()?.span),
            )
        }

        private fun parseBlockOrError(errorMessage: String): LogoBlock? {
            if (!match(LogoTokenType.LEFT_BRACKET)) {
                reportError(errorMessage, peek().span)
                return null
            }
            return parseBracketedBlock(previous())
        }

        private fun parseBracketedBlock(openingBracket: LogoToken): LogoBlock {
            val statements = mutableListOf<LogoStatement>()
            skipNewlines()
            while (!isAtEnd() && !check(LogoTokenType.RIGHT_BRACKET)) {
                val before = current
                val statement = parseStatement(stopTokens = setOf(LogoTokenType.RIGHT_BRACKET))
                if (statement != null) {
                    statements += statement
                }

                if (current == before) {
                    reportError("Unable to parse block statement.", peek().span)
                    advance()
                }
                skipNewlines()
            }

            val closing = if (match(LogoTokenType.RIGHT_BRACKET)) {
                previous()
            } else {
                reportError("Expected closing ']' for block.", peek().span)
                null
            }

            val endSpan = closing?.span ?: statements.lastOrNull()?.span ?: openingBracket.span
            return LogoBlock(
                statements = statements,
                span = spanFrom(openingBracket.span, endSpan),
            )
        }

        private fun parseExpressionOrError(message: String): LogoExpression? {
            val expression = parseExpression()
            if (expression == null) {
                reportError(message, peek().span)
            }
            return expression
        }

        private fun parseExpressionIfPresent(stopTokens: Set<LogoTokenType>): LogoExpression? {
            if (isStatementBoundary(stopTokens)) {
                return null
            }
            return parseExpression()
        }

        private fun parseTrailingArguments(stopTokens: Set<LogoTokenType>): List<LogoExpression> {
            val trailing = mutableListOf<LogoExpression>()
            while (!isStatementBoundary(stopTokens)) {
                val expression = parseExpression()
                if (expression != null) {
                    trailing += expression
                    continue
                }

                val token = advance()
                reportError("Expected expression argument.", token.span)
            }
            return trailing
        }

        private fun parseExpression(): LogoExpression? {
            var expression = parsePrimaryExpression() ?: return null
            while (peek().type.isBinaryOperator()) {
                val operator = advance()
                val right = parsePrimaryExpression()
                if (right == null) {
                    reportError("Expected expression after operator '${operator.lexeme}'.", operator.span)
                    break
                }
                expression = LogoBinaryExpression(
                    left = expression,
                    operator = operator.lexeme,
                    right = right,
                    span = spanFrom(expression.span, right.span),
                )
            }
            return expression
        }

        private fun parsePrimaryExpression(): LogoExpression? {
            return when (peek().type) {
                LogoTokenType.NUMBER -> {
                    val token = advance()
                    LogoNumberExpression(token.lexeme, token.span)
                }

                LogoTokenType.WORD_LITERAL -> {
                    val token = advance()
                    LogoWordExpression(token.lexeme.removePrefix("\""), token.span)
                }

                LogoTokenType.VARIABLE_REFERENCE -> {
                    val token = advance()
                    LogoVariableReferenceExpression(token.lexeme.removePrefix(":"), token.span)
                }

                LogoTokenType.IDENTIFIER -> {
                    val token = advance()
                    if (token.lexeme.equals("thing", ignoreCase = true) && match(LogoTokenType.WORD_LITERAL)) {
                        val target = LogoWordExpression(previous().lexeme.removePrefix("\""), previous().span)
                        return LogoThingExpression(
                            name = target.value,
                            target = target,
                            span = spanFrom(token.span, target.span),
                        )
                    }
                    LogoIdentifierExpression(token.lexeme, token.span)
                }

                LogoTokenType.LEFT_BRACKET -> parseListExpression()
                LogoTokenType.LEFT_PAREN -> parseParenthesizedExpression()
                LogoTokenType.BAD_TOKEN -> {
                    val token = advance()
                    reportError("Unsupported token '${token.lexeme}' in expression.", token.span)
                    LogoBadExpression(token.lexeme, token.span)
                }

                in keywordExpressionTypes -> {
                    val token = advance()
                    LogoIdentifierExpression(token.lexeme, token.span)
                }

                LogoTokenType.NEWLINE,
                LogoTokenType.RIGHT_BRACKET,
                LogoTokenType.RIGHT_PAREN,
                LogoTokenType.EQUAL,
                LogoTokenType.LESS_THAN,
                LogoTokenType.GREATER_THAN,
                LogoTokenType.PLUS,
                LogoTokenType.STAR,
                LogoTokenType.EOF,
                -> null

                else -> {
                    val token = advance()
                    if (token.type in keywordExpressionTypes) {
                        LogoIdentifierExpression(token.lexeme, token.span)
                    } else {
                        reportError("Unexpected token '${token.lexeme}' in expression.", token.span)
                        LogoBadExpression(token.lexeme, token.span)
                    }
                }
            }
        }

        private fun parseListExpression(): LogoListExpression {
            val opening = advance()
            val elements = mutableListOf<LogoExpression>()
            while (!isAtEnd() && !check(LogoTokenType.RIGHT_BRACKET)) {
                if (match(LogoTokenType.NEWLINE)) {
                    continue
                }

                val expression = parseExpression()
                if (expression != null) {
                    elements += expression
                } else {
                    val token = advance()
                    reportError("Expected list element expression.", token.span)
                }
            }

            val closing = if (match(LogoTokenType.RIGHT_BRACKET)) {
                previous()
            } else {
                reportError("Expected closing ']' for list expression.", peek().span)
                null
            }
            return LogoListExpression(
                elements = elements,
                span = spanFrom(opening.span, closing?.span),
            )
        }

        private fun parseParenthesizedExpression(): LogoParenthesizedExpression {
            val opening = advance()
            val expressions = mutableListOf<LogoExpression>()
            while (!isAtEnd() && !check(LogoTokenType.RIGHT_PAREN)) {
                if (match(LogoTokenType.NEWLINE)) {
                    continue
                }

                val expression = parseExpression()
                if (expression != null) {
                    expressions += expression
                } else {
                    val token = advance()
                    reportError("Expected expression inside parentheses.", token.span)
                }
            }

            val closing = if (match(LogoTokenType.RIGHT_PAREN)) {
                previous()
            } else {
                reportError("Expected closing ')' for parenthesized expression.", peek().span)
                null
            }
            return LogoParenthesizedExpression(
                expressions = expressions,
                span = spanFrom(opening.span, closing?.span),
            )
        }

        private fun isStatementBoundary(stopTokens: Set<LogoTokenType>): Boolean {
            val type = peek().type
            return type == LogoTokenType.NEWLINE ||
                type == LogoTokenType.EOF ||
                type == LogoTokenType.RIGHT_BRACKET ||
                type in stopTokens
        }

        private fun skipNewlines() {
            while (match(LogoTokenType.NEWLINE)) {
                // Skip blank lines between statements.
            }
        }

        private fun check(type: LogoTokenType): Boolean {
            return !isAtEnd() && peek().type == type
        }

        private fun match(type: LogoTokenType): Boolean {
            if (!check(type)) {
                return false
            }
            advance()
            return true
        }

        private fun LogoTokenType.isBinaryOperator(): Boolean {
            return this == LogoTokenType.EQUAL ||
                this == LogoTokenType.LESS_THAN ||
                this == LogoTokenType.GREATER_THAN ||
                this == LogoTokenType.PLUS ||
                this == LogoTokenType.STAR
        }

        private fun advance(): LogoToken {
            if (!isAtEnd()) {
                current += 1
            }
            return previous()
        }

        private fun isAtEnd(): Boolean = peek().type == LogoTokenType.EOF

        private fun peek(): LogoToken = tokens[current]

        private fun peekNext(): LogoToken? = tokens.getOrNull(current + 1)

        private fun previous(): LogoToken = tokens[current - 1]

        private fun reportError(message: String, span: SourceSpan) {
            errors += LogoParseError(message, span)
        }

        private fun spanFrom(start: SourceSpan, end: SourceSpan?): SourceSpan {
            val endPosition = end?.end ?: start.end
            return SourceSpan(start.start, endPosition)
        }
    }
}
