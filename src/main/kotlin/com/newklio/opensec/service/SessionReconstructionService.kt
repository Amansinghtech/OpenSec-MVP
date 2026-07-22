package com.newklio.opensec.service

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.config.SessionProperties
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SESSION_UPDATED_EVENT
import com.newklio.opensec.dto.SUSPICIOUS_SESSION_EVENT
import com.newklio.opensec.dto.SessionEventRef
import com.newklio.opensec.dto.SessionSummary
import com.newklio.opensec.entity.Session
import com.newklio.opensec.model.NormalizedEventType
import com.newklio.opensec.model.SessionStatus
import com.newklio.opensec.repository.SessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.UUID

/**
 * Reconstructs behavioral sessions from the normalized event stream (PRD §5). Maintains a sliding
 * activity window per `(tenant, entity_type, entity_id)`, appends events, and flags suspicious
 * state transitions (brute force, successful brute force, privilege escalation).
 */
@Service
class SessionReconstructionService(
    private val sessionRepository: SessionRepository,
    private val eventPublisher: EventPublisher,
    private val objectMapper: ObjectMapper,
    private val properties: SessionProperties,
) {
    private val sequenceType = object : TypeReference<MutableList<SessionEventRef>>() {}

    @Transactional
    fun process(event: NormalizedEvent): Session {
        val session = resolveSession(event)

        val sequence = readSequence(session.sequenceJson)
        sequence.add(SessionEventRef(event.eventType, event.occurredAt, event.severity))
        session.sequenceJson = objectMapper.writeValueAsString(sequence)
        session.eventCount = sequence.size
        if (event.occurredAt.isAfter(session.lastEventAt)) {
            session.lastEventAt = event.occurredAt
        }
        if (event.eventType == NormalizedEventType.AUTH_FAILURE) {
            session.authFailureCount += 1
        }

        val wasSuspicious = session.suspicious
        evaluate(session, sequence)
        val saved = sessionRepository.save(session)

        publish(KafkaTopics.SESSION_EVENTS, SESSION_UPDATED_EVENT, saved)
        if (saved.suspicious && !wasSuspicious) {
            publish(KafkaTopics.SESSION_EVENTS, SUSPICIOUS_SESSION_EVENT, saved)
        }
        return saved
    }

    private fun resolveSession(event: NormalizedEvent): Session {
        val open =
            sessionRepository.findFirstByTenantIdAndEntityTypeAndEntityIdAndStatusOrderByLastEventAtDesc(
                event.tenantId,
                event.entityType,
                event.entityId,
                SessionStatus.OPEN,
            )
        if (open != null) {
            if (isExpired(open, event)) {
                open.status = SessionStatus.CLOSED
                sessionRepository.save(open)
            } else {
                return open
            }
        }
        return Session(
            tenantId = event.tenantId,
            entityType = event.entityType,
            entityId = event.entityId,
            startedAt = event.occurredAt,
            lastEventAt = event.occurredAt,
        )
    }

    private fun isExpired(
        session: Session,
        event: NormalizedEvent,
    ): Boolean = Duration.between(session.lastEventAt, event.occurredAt) > Duration.ofMinutes(properties.windowMinutes)

    private fun evaluate(
        session: Session,
        sequence: List<SessionEventRef>,
    ) {
        val reasons = mutableListOf<String>()

        if (session.authFailureCount >= properties.bruteForceThreshold) {
            reasons.add("possible brute force: ${session.authFailureCount} auth failures")
        }

        val firstSuccess = sequence.indexOfFirst { it.eventType == NormalizedEventType.AUTH_SUCCESS }
        if (firstSuccess >= 0) {
            val failuresBefore = sequence.take(firstSuccess).count { it.eventType == NormalizedEventType.AUTH_FAILURE }
            if (failuresBefore >= properties.bruteForceSuccessThreshold) {
                reasons.add("possible successful brute force after $failuresBefore failures")
            }
            val escalated = sequence.drop(firstSuccess).any { it.eventType == NormalizedEventType.PRIVILEGE_ESCALATION }
            if (escalated) {
                reasons.add("privilege escalation after authentication")
            }
        }

        if (reasons.isNotEmpty()) {
            session.suspicious = true
            session.riskReason = reasons.joinToString("; ")
        }
    }

    private fun readSequence(json: String): MutableList<SessionEventRef> =
        runCatching { objectMapper.readValue(json, sequenceType) }.getOrElse { mutableListOf() }

    private fun publish(
        topic: String,
        eventType: String,
        session: Session,
    ) {
        val summary =
            SessionSummary(
                sessionId = session.id ?: UUID.randomUUID(),
                tenantId = session.tenantId,
                entityType = session.entityType,
                entityId = session.entityId,
                status = session.status,
                eventCount = session.eventCount,
                authFailureCount = session.authFailureCount,
                suspicious = session.suspicious,
                riskReason = session.riskReason,
                startedAt = session.startedAt,
                lastEventAt = session.lastEventAt,
            )

        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.convertValue(summary, Map::class.java) as Map<String, Any?>
        eventPublisher.publish(
            topic,
            EventEnvelope(
                eventType = eventType,
                tenantId = session.tenantId,
                source = "session-reconstruction",
                occurredAt = session.lastEventAt,
                payload = payload,
            ),
        )
    }
}
