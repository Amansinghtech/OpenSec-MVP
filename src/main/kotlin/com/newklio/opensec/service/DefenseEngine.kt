package com.newklio.opensec.service

import com.newklio.opensec.config.DefenseProperties
import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.Alert
import com.newklio.opensec.dto.DEFENSE_ACTION_EXECUTED_EVENT
import com.newklio.opensec.dto.DefenseActionResult
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.entity.DefenseActionRecord
import com.newklio.opensec.model.DefenseActionStatus
import com.newklio.opensec.model.DefenseActionType
import com.newklio.opensec.model.SignalSeverity
import com.newklio.opensec.repository.DefenseActionRepository
import com.newklio.opensec.service.defense.DefenseExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class DefenseEngine(
    private val executors: List<DefenseExecutor>,
    private val defenseActionRepository: DefenseActionRepository,
    private val eventPublisher: EventPublisher,
    private val objectMapper: ObjectMapper,
    private val properties: DefenseProperties,
    private val notifier: DefenseNotifier,
) {
    @Transactional
    fun evaluateAlert(alert: Alert): List<DefenseActionResult> {
        if (!properties.enabled || alert.riskScore < properties.minRiskScore) return emptyList()

        val actions = selectActions(alert)
        return actions.mapNotNull { type ->
            val executor = executors.firstOrNull { it.actionType == type } ?: return@mapNotNull null
            val detail = executor.execute(alert, properties.dryRun)
            val result =
                DefenseActionResult(
                    tenantId = alert.tenantId,
                    alertId = alert.id,
                    correlationId = alert.correlationId,
                    actionType = type,
                    status = if (properties.dryRun) DefenseActionStatus.DRY_RUN else DefenseActionStatus.EXECUTED,
                    target = alert.entityId,
                    detail = detail,
                    dryRun = properties.dryRun,
                )
            defenseActionRepository.save(result.toRecord())
            publish(result)
            notifier.notify(result)
            result
        }
    }

    @Transactional(readOnly = true)
    fun listActions(tenantId: java.util.UUID): List<DefenseActionResult> =
        defenseActionRepository.findByTenantId(tenantId).map { it.toDto() }

    private fun selectActions(alert: Alert): List<DefenseActionType> =
        when (alert.severity) {
            SignalSeverity.CRITICAL -> listOf(DefenseActionType.BLOCK_IP, DefenseActionType.REVOKE_TOKENS)
            SignalSeverity.HIGH -> listOf(DefenseActionType.DISABLE_USER)
            else -> emptyList()
        }

    private fun publish(result: DefenseActionResult) {
        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.convertValue(result, Map::class.java) as Map<String, Any?>
        eventPublisher.publish(
            KafkaTopics.DEFENSE_EVENTS,
            EventEnvelope(
                eventType = DEFENSE_ACTION_EXECUTED_EVENT,
                tenantId = result.tenantId,
                correlationId = result.correlationId,
                source = "defense-engine",
                occurredAt = result.executedAt,
                payload = payload,
                eventId = result.id,
            ),
        )
    }

    private fun DefenseActionResult.toRecord() =
        DefenseActionRecord(
            id = id,
            tenantId = tenantId,
            alertId = alertId,
            correlationId = correlationId,
            actionType = actionType,
            status = status,
            target = target,
            detail = detail,
            dryRun = dryRun,
            executedAt = executedAt,
        )

    private fun DefenseActionRecord.toDto() =
        DefenseActionResult(
            id = id,
            tenantId = tenantId,
            alertId = alertId,
            correlationId = correlationId,
            actionType = actionType,
            status = status,
            target = target,
            detail = detail,
            dryRun = dryRun,
            executedAt = executedAt,
        )
}

interface DefenseNotifier {
    fun notify(result: DefenseActionResult)
}

@Service
class LoggingDefenseNotifier : DefenseNotifier {
    private val log = org.slf4j.LoggerFactory.getLogger(LoggingDefenseNotifier::class.java)

    override fun notify(result: DefenseActionResult) {
        log.info("Defense action {} for alert {}: {}", result.actionType, result.alertId, result.detail)
    }
}
