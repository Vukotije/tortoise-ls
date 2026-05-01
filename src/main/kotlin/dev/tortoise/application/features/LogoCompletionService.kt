package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.language.completion.LogoCompletionEngine
import dev.tortoise.shared.model.LogoCompletionItem
import dev.tortoise.shared.text.SourcePosition

class LogoCompletionService(
    private val engine: LogoCompletionEngine = LogoCompletionEngine(),
) : CompletionService {
    override fun completions(analysis: DocumentAnalysis, position: SourcePosition): List<LogoCompletionItem> {
        return engine.complete(
            text = analysis.text,
            parseResult = analysis.parseResult,
            resolutionResult = analysis.resolutionResult,
            position = position,
        )
    }
}
