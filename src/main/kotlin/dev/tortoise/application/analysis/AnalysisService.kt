package dev.tortoise.application.analysis

import dev.tortoise.application.documents.DocumentSnapshot

interface AnalysisService {
    fun analyze(document: DocumentSnapshot): DocumentAnalysis

    fun getCached(uri: String): DocumentAnalysis?

    fun clear(uri: String)
}
