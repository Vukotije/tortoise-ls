package dev.tortoise.application.analysis

import dev.tortoise.application.documents.DocumentSnapshot
import dev.tortoise.language.diagnostics.DiagnosticsService
import dev.tortoise.language.diagnostics.LogoDiagnosticsService
import dev.tortoise.language.parser.TurtleLogoParser
import dev.tortoise.language.resolve.TurtleLogoResolver

class CachedAnalysisService(
    private val parser: TurtleLogoParser = TurtleLogoParser(),
    private val resolver: TurtleLogoResolver = TurtleLogoResolver(),
    private val diagnosticsService: DiagnosticsService = LogoDiagnosticsService(),
) : AnalysisService {
    private val analysisCache = LinkedHashMap<String, DocumentAnalysis>()

    override fun analyze(document: DocumentSnapshot): DocumentAnalysis {
        val cached = analysisCache[document.uri]
        if (cached != null && cached.version == document.version) {
            return cached
        }

        val parseResult = parser.parse(document.text)
        val resolutionResult = resolver.resolve(parseResult.program)
        val diagnostics = diagnosticsService.computeDiagnostics(parseResult, resolutionResult)
        return DocumentAnalysis(
            uri = document.uri,
            version = document.version,
            text = document.text,
            parseResult = parseResult,
            resolutionResult = resolutionResult,
            diagnostics = diagnostics,
        ).also { analysis ->
            analysisCache[document.uri] = analysis
        }
    }

    override fun getCached(uri: String): DocumentAnalysis? = analysisCache[uri]

    override fun clear(uri: String) {
        analysisCache.remove(uri)
    }
}
