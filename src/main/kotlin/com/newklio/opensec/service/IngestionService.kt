package com.newklio.opensec.service

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.context.RequestContext
import com.newklio.opensec.dto.BatchIngestResult
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.IngestLogRequest
import com.newklio.opensec.dto.IngestResult
import com.newklio.opensec.entity.RawLog
import com.newklio.opensec.repository.RawLogRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

const val RAW_LOG_INGESTED_EVENT = "raw_log.ingested"

@Service
class IngestionService(
    private val rawLogRepository: RawLogRepository,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: EventPublisher,
) {
    private val log = LoggerFactory.getLogger(IngestionService::class.java)

    @Transactional
    fun ingest(request: IngestLogRequest): IngestResult {
        require(request.source.isNotBlank()) { "source is required" }
        val tenantId = currentTenantId()

        if (isDuplicate(tenantId, request)) {
            return IngestResult(id = null, duplicate = true)
        }

        return try {
            val saved = rawLogRepository.save(toRawLog(request, tenantId))
            publish(request, tenantId)
            IngestResult(id = saved.id, duplicate = false)
        } catch (ex: DataIntegrityViolationException) {
            // Lost a dedupe race against a concurrent insert of the same event.
            log.debug("Duplicate raw log for source={} eventId={}", request.source, request.eventId)
            IngestResult(id = null, duplicate = true)
        }
    }

    @Transactional
    fun ingestBatch(requests: List<IngestLogRequest>): BatchIngestResult {
        val tenantId = currentTenantId()
        val ids = mutableListOf<UUID>()
        var duplicates = 0
        // Track keys seen earlier in this batch so identical events in one request are deduped too.
        val seen = mutableSetOf<String>()

        for (request in requests) {
            require(request.source.isNotBlank()) { "source is required" }
            val key = request.eventId?.let { "${request.source}::$it" }
            if (key != null && (!seen.add(key) || isDuplicate(tenantId, request))) {
                duplicates++
                continue
            }
            val saved = rawLogRepository.save(toRawLog(request, tenantId))
            publish(request, tenantId)
            saved.id?.let { ids.add(it) }
        }
        return BatchIngestResult(accepted = ids.size, duplicates = duplicates, ids = ids)
    }

    private fun publish(
        request: IngestLogRequest,
        tenantId: UUID,
    ) {
        val envelope =
            EventEnvelope(
                eventType = RAW_LOG_INGESTED_EVENT,
                tenantId = tenantId,
                correlationId = RequestContext.getCorrelationId(),
                source = request.source,
                occurredAt = request.occurredAt ?: Instant.now(),
                payload = request.payload,
            )
        eventPublisher.publish(KafkaTopics.RAW_LOGS, envelope)
    }

    private fun isDuplicate(
        tenantId: UUID,
        request: IngestLogRequest,
    ): Boolean =
        request.eventId != null &&
            rawLogRepository.existsByTenantIdAndSourceAndEventId(tenantId, request.source, request.eventId)

    private fun toRawLog(
        request: IngestLogRequest,
        tenantId: UUID,
    ): RawLog =
        RawLog(
            source = request.source,
            eventId = request.eventId,
            host = request.host,
            tenantId = tenantId,
            correlationId = RequestContext.getCorrelationId(),
            severity = request.severity,
            category = request.category,
            occurredAt = request.occurredAt ?: Instant.now(),
            payload = objectMapper.writeValueAsString(request.payload),
        )

    private fun currentTenantId(): UUID =
        RequestContext.getTenantId()
            ?: throw IllegalStateException("No tenant context; ingestion requires an authenticated tenant-scoped principal")
}
