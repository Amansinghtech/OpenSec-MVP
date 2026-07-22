package com.newklio.opensec.rag

import com.newklio.opensec.dto.DetectionSignal

interface LlmProvider {
    fun enrich(
        signal: DetectionSignal,
        similarExploits: List<ExploitDocument>,
    ): String?
}
