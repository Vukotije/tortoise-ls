package dev.tortoise.application.features

import dev.tortoise.application.analysis.DocumentAnalysis
import dev.tortoise.shared.model.LogoSemanticTokens

interface SemanticTokensService {
    fun semanticTokens(analysis: DocumentAnalysis): LogoSemanticTokens
}
