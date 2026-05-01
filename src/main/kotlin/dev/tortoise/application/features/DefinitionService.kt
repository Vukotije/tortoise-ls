package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.shared.text.SourcePosition
import dev.tortoise.shared.text.SourceSpan

interface DefinitionService {
    fun definition(analysis: DocumentAnalysis, position: SourcePosition): SourceSpan?
}
