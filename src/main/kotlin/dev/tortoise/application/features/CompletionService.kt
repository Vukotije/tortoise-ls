package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.shared.model.LogoCompletionItem
import dev.tortoise.shared.text.SourcePosition

interface CompletionService {
    fun completions(analysis: DocumentAnalysis, position: SourcePosition): List<LogoCompletionItem>
}
