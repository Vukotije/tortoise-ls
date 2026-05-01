package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

interface ReferenceService {
    fun references(
        analysis: DocumentAnalysis,
        position: SourcePosition,
        includeDeclaration: Boolean,
    ): List<SourceSpan>
}
