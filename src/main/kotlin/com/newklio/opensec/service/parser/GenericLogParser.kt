package com.newklio.opensec.service.parser

import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Fallback parser for arbitrary/custom sources. Best-effort extraction of a primary entity and
 * severity from common field names. Used when no source-specific parser matches.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class GenericLogParser : LogParser {
    override fun supports(source: String): Boolean = true

    override fun parse(envelope: EventEnvelope): NormalizedEvent {
        val payload = envelope.payload
        val host = payload["host"] as? String
        val (entityType, entityId) = resolveEntity(payload, host)

        return NormalizedEvent(
            eventId = envelope.eventId,
            tenantId = envelope.tenantId,
            correlationId = envelope.correlationId,
            source = envelope.source ?: "unknown",
            eventType = classify(payload),
            category = payload["category"] as? String,
            severity = (payload["severity"] as? Number)?.toInt(),
            host = host,
            entityType = entityType,
            entityId = entityId,
            occurredAt = envelope.occurredAt,
            attributes = payload,
        )
    }

    private fun resolveEntity(
        payload: Map<String, Any?>,
        host: String?,
    ): Pair<EntityType, String> {
        val ip = (payload["srcip"] ?: payload["ip"] ?: payload["source_ip"]) as? String
        val user = (payload["user"] ?: payload["username"]) as? String
        return when {
            !ip.isNullOrBlank() -> EntityType.IP to ip
            !user.isNullOrBlank() -> EntityType.USER to user
            !host.isNullOrBlank() -> EntityType.HOST to host
            else -> EntityType.UNKNOWN to "unknown"
        }
    }

    private fun classify(payload: Map<String, Any?>): NormalizedEventType {
        val hint = (payload["eventType"] as? String) ?: (payload["type"] as? String) ?: return NormalizedEventType.GENERIC
        return runCatching { NormalizedEventType.valueOf(hint.uppercase()) }.getOrDefault(NormalizedEventType.GENERIC)
    }
}
