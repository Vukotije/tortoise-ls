package dev.tortoise.application.documents

interface TextSyncStrategy {
    fun apply(
        current: DocumentSnapshot,
        newVersion: Int,
        changes: List<TextDocumentChange>,
    ): DocumentSnapshot
}
