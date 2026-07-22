package com.newklio.opensec.service

import com.newklio.opensec.dto.EventEnvelope

/**
 * Publishes events to the async event bus. Implementations must be non-blocking and fail-safe:
 * a bus outage must never break the request path (PRD: AI/eventing is augmenting, not blocking).
 */
interface EventPublisher {
    fun publish(
        topic: String,
        envelope: EventEnvelope,
    )
}
