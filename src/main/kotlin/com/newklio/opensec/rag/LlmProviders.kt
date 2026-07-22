package com.newklio.opensec.rag

import com.newklio.opensec.dto.DetectionSignal
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["opensec.rag.enabled"], havingValue = "false", matchIfMissing = true)
class NoopLlmProvider : LlmProvider {
    override fun enrich(
        signal: DetectionSignal,
        similarExploits: List<ExploitDocument>,
    ): String? = null
}

@Component
@ConditionalOnProperty(name = ["opensec.rag.enabled"], havingValue = "true")
class HeuristicLlmProvider : LlmProvider {
    override fun enrich(
        signal: DetectionSignal,
        similarExploits: List<ExploitDocument>,
    ): String? {
        if (similarExploits.isEmpty()) {
            return "No matching exploit corpus entries for '${signal.reason}'."
        }
        val cves = similarExploits.joinToString { it.cveId }
        return "Detection '${signal.signalType}' may relate to known exploits: $cves. Review ${signal.entityId}."
    }
}
