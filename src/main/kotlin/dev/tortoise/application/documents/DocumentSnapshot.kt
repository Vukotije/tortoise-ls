package dev.tortoise.application.documents

data class DocumentSnapshot(
    val uri: String,
    val version: Int,
    val text: String,
)
