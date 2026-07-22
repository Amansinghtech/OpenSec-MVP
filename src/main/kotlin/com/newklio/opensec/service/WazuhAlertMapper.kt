package com.newklio.opensec.service

import com.newklio.opensec.dto.IngestLogRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Maps a Wazuh Integrator alert payload (posted to the Wazuh ingestion webhook) into the
 * source-agnostic [IngestLogRequest]. The full alert is preserved as the payload.
 */
@Component
class WazuhAlertMapper {
    private val log = LoggerFactory.getLogger(WazuhAlertMapper::class.java)

    // Wazuh timestamps look like 2024-01-01T12:00:00.000+0000 (offset without a colon).
    private val wazuhTimestampFormat =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]xx")

    @Suppress("UNCHECKED_CAST")
    fun toIngestRequest(alert: Map<String, Any?>): IngestLogRequest {
        val rule = alert["rule"] as? Map<String, Any?> ?: emptyMap()
        val agent = alert["agent"] as? Map<String, Any?> ?: emptyMap()

        val host = (agent["name"] as? String) ?: (agent["ip"] as? String)
        val severity = (rule["level"] as? Number)?.toInt()
        val category =
            (rule["description"] as? String)
                ?: (rule["groups"] as? List<*>)?.joinToString(",")

        return IngestLogRequest(
            source = "wazuh",
            host = host,
            eventId = alert["id"]?.toString(),
            occurredAt = parseTimestamp(alert["timestamp"] as? String),
            severity = severity,
            category = category,
            payload = alert,
        )
    }

    private fun parseTimestamp(timestamp: String?): Instant {
        if (timestamp.isNullOrBlank()) return Instant.now()
        return runCatching { Instant.parse(timestamp) }
            .recoverCatching { OffsetDateTime.parse(timestamp, wazuhTimestampFormat).toInstant() }
            .getOrElse {
                log.warn("Unparseable Wazuh timestamp '{}', using now()", timestamp)
                Instant.now()
            }
    }
}
