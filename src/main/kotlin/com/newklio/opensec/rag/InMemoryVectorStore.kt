package com.newklio.opensec.rag

import org.springframework.stereotype.Component

@Component
class InMemoryVectorStore : VectorStore {
    private val corpus =
        listOf(
            ExploitDocument(
                id = "exp-1",
                cveId = "CVE-2021-44228",
                title = "Log4Shell",
                description = "Remote code execution via JNDI lookup in Apache Log4j",
            ),
            ExploitDocument(
                id = "exp-2",
                cveId = "CVE-2023-23397",
                title = "Outlook elevation",
                description = "Microsoft Outlook privilege escalation via NTLM relay",
            ),
            ExploitDocument(
                id = "exp-3",
                cveId = "CVE-2020-1472",
                title = "Zerologon",
                description = "Netlogon elevation of privilege in Windows domain controllers",
            ),
        )

    override fun search(
        query: String,
        topK: Int,
    ): List<SimilarityHit> {
        val normalized = query.lowercase()
        return corpus
            .map { doc ->
                val haystack = "${doc.title} ${doc.description} ${doc.cveId}".lowercase()
                val score =
                    normalized
                        .split(Regex("\\s+"))
                        .count { token -> token.length > 2 && haystack.contains(token) }
                        .toDouble()
                SimilarityHit(doc, score)
            }.filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(topK)
    }
}
