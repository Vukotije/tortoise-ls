package dev.tortoise.application.documents

interface DocumentStore {
    fun open(uri: String, version: Int, text: String): DocumentSnapshot

    fun change(uri: String, version: Int, changes: List<TextDocumentChange>): DocumentSnapshot

    fun close(uri: String)

    fun get(uri: String): DocumentSnapshot?
}
