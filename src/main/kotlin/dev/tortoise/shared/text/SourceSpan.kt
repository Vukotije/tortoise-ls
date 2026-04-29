package dev.tortoise.shared.text

data class SourceSpan(
    val start: SourcePosition,
    val end: SourcePosition,
) {
    init {
        require(start.offset <= end.offset) {
            "Span start offset must not be after end offset."
        }
    }
}
