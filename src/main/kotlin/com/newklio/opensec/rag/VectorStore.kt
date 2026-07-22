package com.newklio.opensec.rag

data class ExploitDocument(
    val id: String,
    val cveId: String,
    val title: String,
    val description: String,
)

data class SimilarityHit(
    val document: ExploitDocument,
    val score: Double,
)

interface VectorStore {
    fun search(
        query: String,
        topK: Int,
    ): List<SimilarityHit>
}
