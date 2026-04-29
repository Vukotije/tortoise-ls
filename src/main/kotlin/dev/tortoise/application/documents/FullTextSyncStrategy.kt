package dev.tortoise.application.documents

class FullTextSyncStrategy : TextSyncStrategy {
    override fun apply(
        current: DocumentSnapshot,
        newVersion: Int,
        changes: List<TextDocumentChange>,
    ): DocumentSnapshot {
        require(changes.isNotEmpty()) { "Full sync changes must include replacement text." }

        return current.copy(
            version = newVersion,
            text = changes.last().text,
        )
    }
}
