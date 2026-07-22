package com.newklio.opensec.service.parser

import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent

/**
 * Pluggable parser that converts a raw ingestion [EventEnvelope] into a [NormalizedEvent].
 * New sources are added by implementing this interface (foundation for the Phase 17 plugin system).
 */
interface LogParser {
    fun supports(source: String): Boolean

    fun parse(envelope: EventEnvelope): NormalizedEvent
}
