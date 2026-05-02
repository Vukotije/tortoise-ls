package dev.tortoise.server.protocol

import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

internal fun SourcePosition.toLspPosition(): Position {
    return Position(
        (line - 1).coerceAtLeast(0),
        (column - 1).coerceAtLeast(0),
    )
}

internal fun SourceSpan.toLspRange(): Range {
    return Range(start.toLspPosition(), end.toLspPosition())
}

internal fun String.toSourcePosition(line: Int, character: Int): SourcePosition {
    val targetLine = (line + 1).coerceAtLeast(1)
    val targetColumn = (character + 1).coerceAtLeast(1)
    var currentLine = 1
    var currentColumn = 1
    var offset = 0

    while (offset < length && (currentLine < targetLine || currentColumn < targetColumn)) {
        val char = this[offset]
        offset += 1
        if (char == '\r') {
            if (offset < length && this[offset] == '\n') {
                offset += 1
            }
            currentLine += 1
            currentColumn = 1
        } else if (char == '\n') {
            currentLine += 1
            currentColumn = 1
        } else {
            currentColumn += 1
        }
    }

    return SourcePosition(offset = offset, line = targetLine, column = targetColumn)
}
