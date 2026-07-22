package com.newklio.opensec.service.parser

import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Parses Wazuh Integrator alert payloads. Wazuh already emits structured JSON, so this is mostly
 * field extraction + mapping rule groups to our [NormalizedEventType] taxonomy.
 */
@Component
@Order(0)
class WazuhLogParser : LogParser {
    override fun supports(source: String): Boolean = source.equals("wazuh", ignoreCase = true)

    @Suppress("UNCHECKED_CAST")
    override fun parse(envelope: EventEnvelope): NormalizedEvent {
        val payload = envelope.payload
        val rule = payload["rule"] as? Map<String, Any?> ?: emptyMap()
        val agent = payload["agent"] as? Map<String, Any?> ?: emptyMap()
        val data = payload["data"] as? Map<String, Any?> ?: emptyMap()

        val groups = (rule["groups"] as? List<*>)?.map { it.toString().lowercase() } ?: emptyList()
        val eventType = classify(groups)
        val severity = (rule["level"] as? Number)?.toInt()
        val host = (agent["name"] as? String) ?: (agent["ip"] as? String)

        val (entityType, entityId) = resolveEntity(data, host)

        return NormalizedEvent(
            eventId = envelope.eventId,
            tenantId = envelope.tenantId,
            correlationId = envelope.correlationId,
            source = "wazuh",
            eventType = eventType,
            category = rule["description"] as? String ?: groups.joinToString(","),
            severity = severity,
            host = host,
            entityType = entityType,
            entityId = entityId,
            occurredAt = envelope.occurredAt,
            attributes =
                buildMap {
                    put("ruleId", rule["id"])
                    put("ruleGroups", groups)
                    put("mitre", rule["mitre"])
                    data["srcip"]?.let { put("srcip", it) }
                    (data["srcuser"] ?: data["dstuser"] ?: data["user"])?.let { put("user", it) }
                    agent["id"]?.let { put("agentId", it) }
                },
        )
    }

    private fun resolveEntity(
        data: Map<String, Any?>,
        host: String?,
    ): Pair<EntityType, String> {
        val srcip = data["srcip"] as? String
        val user = (data["srcuser"] ?: data["dstuser"] ?: data["user"]) as? String
        return when {
            !srcip.isNullOrBlank() -> EntityType.IP to srcip
            !user.isNullOrBlank() -> EntityType.USER to user
            !host.isNullOrBlank() -> EntityType.HOST to host
            else -> EntityType.UNKNOWN to "unknown"
        }
    }

    private fun classify(groups: List<String>): NormalizedEventType {
        fun has(vararg needles: String) = groups.any { g -> needles.any { g.contains(it) } }
        return when {
            has("authentication_success", "authentication_succeeded") -> NormalizedEventType.AUTH_SUCCESS
            has("authentication_failed", "authentication_failures", "invalid_login", "win_authentication_failed") ->
                NormalizedEventType.AUTH_FAILURE
            has("privilege_escalation", "adduser", "account_changed", "policy_changed") ->
                NormalizedEventType.PRIVILEGE_ESCALATION
            has("syscheck", "file_integrity") -> NormalizedEventType.FILE_INTEGRITY
            has("virus", "malware", "rootcheck", "trojan") -> NormalizedEventType.MALWARE
            has("web", "sql_injection", "xss", "attack") -> NormalizedEventType.WEB_ATTACK
            has("ids", "intrusion_detection") -> NormalizedEventType.INTRUSION
            has("firewall", "network", "packetbeat") -> NormalizedEventType.NETWORK
            has("process", "audit") -> NormalizedEventType.PROCESS
            else -> NormalizedEventType.GENERIC
        }
    }
}
