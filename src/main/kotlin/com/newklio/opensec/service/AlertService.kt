package com.newklio.opensec.service

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.ALERT_CREATED_EVENT
import com.newklio.opensec.dto.Alert
import com.newklio.opensec.dto.ContextualizedDetection
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.entity.AlertRecord
import com.newklio.opensec.model.AlertStatus
import com.newklio.opensec.repository.AlertRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val eventPublisher: EventPublisher,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun createFromSignal(signal: DetectionSignal): Alert = create(buildFromSignal(signal))

    @Transactional
    fun createFromContextualized(detection: ContextualizedDetection): Alert = create(buildFromContextualized(detection))

    @Transactional(readOnly = true)
    fun listAlerts(tenantId: UUID): List<Alert> = alertRepository.findByTenantId(tenantId).map { it.toDto() }

    @Transactional(readOnly = true)
    fun getAlert(
        id: UUID,
        tenantId: UUID,
    ): Alert? = alertRepository.findByIdAndTenantId(id, tenantId)?.toDto()

    @Transactional
    fun updateStatus(
        id: UUID,
        tenantId: UUID,
        status: AlertStatus,
    ): Alert? {
        val record =
            alertRepository.findByIdAndTenantId(id, tenantId)
                ?: return null
        record.status = status
        record.updatedAt = Instant.now()
        return alertRepository.save(record).toDto()
    }

    private fun create(alert: Alert): Alert {
        alertRepository.save(alert.toRecord())
        publish(alert)
        return alert
    }

    private fun buildFromSignal(signal: DetectionSignal): Alert =
        Alert(
            tenantId = signal.tenantId,
            correlationId = signal.correlationId,
            title = "${signal.signalType} on ${signal.entityId}",
            description = signal.reason,
            entityType = signal.entityType,
            entityId = signal.entityId,
            riskScore = signal.riskScore,
            severity = signal.severity,
            signalIds = listOf(signal.signalId),
            createdAt = signal.occurredAt,
        )

    private fun buildFromContextualized(detection: ContextualizedDetection): Alert {
        val boosted = detection.riskScore + detection.cveIds.size * 5
        return Alert(
            tenantId = detection.tenantId,
            correlationId = detection.correlationId,
            title = "Contextualized: ${detection.detector}",
            description = detection.contextSummary ?: detection.reason,
            entityType = com.newklio.opensec.model.EntityType.HOST,
            entityId = detection.detector,
            riskScore = boosted.coerceAtMost(100),
            severity = detection.severity,
            signalIds = listOf(detection.signalId),
            cveIds = detection.cveIds,
            createdAt = detection.occurredAt,
        )
    }

    private fun publish(alert: Alert) {
        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.convertValue(alert, Map::class.java) as Map<String, Any?>
        eventPublisher.publish(
            KafkaTopics.ALERT_EVENTS,
            EventEnvelope(
                eventType = ALERT_CREATED_EVENT,
                tenantId = alert.tenantId,
                correlationId = alert.correlationId,
                source = "alert-service",
                occurredAt = alert.createdAt,
                payload = payload,
                eventId = alert.id,
            ),
        )
    }

    private fun Alert.toRecord() =
        AlertRecord(
            id = id,
            tenantId = tenantId,
            correlationId = correlationId,
            title = title,
            description = description,
            entityType = entityType,
            entityId = entityId,
            riskScore = riskScore,
            severity = severity,
            status = status,
            signalIds = signalIds.joinToString(","),
            cveIds = cveIds.takeIf { it.isNotEmpty() }?.joinToString(","),
        )

    private fun AlertRecord.toDto() =
        Alert(
            id = id,
            tenantId = tenantId,
            correlationId = correlationId,
            title = title,
            description = description,
            entityType = entityType,
            entityId = entityId,
            riskScore = riskScore,
            severity = severity,
            status = status,
            signalIds = signalIds.split(",").filter { it.isNotBlank() }.map { UUID.fromString(it) },
            cveIds = cveIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            createdAt = createdAt ?: Instant.now(),
            updatedAt = updatedAt ?: Instant.now(),
        )
}
