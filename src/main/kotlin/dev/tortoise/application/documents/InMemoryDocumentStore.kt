package dev.tortoise.application.documents

class InMemoryDocumentStore(
    private val textSyncStrategy: TextSyncStrategy,
) : DocumentStore {
    private val documents = LinkedHashMap<String, DocumentSnapshot>()

    override fun open(uri: String, version: Int, text: String): DocumentSnapshot {
        val snapshot = DocumentSnapshot(uri = uri, version = version, text = text)
        documents[uri] = snapshot
        return snapshot
    }

    override fun change(uri: String, version: Int, changes: List<TextDocumentChange>): DocumentSnapshot {
        val current = documents[uri] ?: error("Cannot apply change to unopened document: $uri")
        val updated = textSyncStrategy.apply(current, version, changes)
        documents[uri] = updated
        return updated
    }

    override fun close(uri: String) {
        documents.remove(uri)
    }

    override fun get(uri: String): DocumentSnapshot? = documents[uri]
}
