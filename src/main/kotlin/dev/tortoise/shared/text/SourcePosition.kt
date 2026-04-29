package dev.tortoise.shared.text

data class SourcePosition(
    val offset: Int,
    val line: Int,
    val column: Int,
)
