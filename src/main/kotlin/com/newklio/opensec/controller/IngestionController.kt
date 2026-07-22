package com.newklio.opensec.controller

import com.newklio.opensec.dto.BatchIngestResult
import com.newklio.opensec.dto.IngestLogRequest
import com.newklio.opensec.dto.IngestResult
import com.newklio.opensec.service.IngestionService
import com.newklio.opensec.service.WazuhAlertMapper
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Ingestion API (PRD §3). Endpoints require an `AGENT` (or `ADMIN`) role; the tenant is derived
 * from the authenticated principal via the request context, never from the client payload.
 */
@RestController
@RequestMapping("/ingest")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
class IngestionController(
    private val ingestionService: IngestionService,
    private val wazuhAlertMapper: WazuhAlertMapper,
) {
    @PostMapping("/logs")
    fun ingest(
        @Valid @RequestBody request: IngestLogRequest,
    ): IngestResult = ingestionService.ingest(request)

    @PostMapping("/logs/batch")
    fun ingestBatch(
        @RequestBody requests: List<IngestLogRequest>,
    ): BatchIngestResult = ingestionService.ingestBatch(requests)

    @PostMapping("/wazuh")
    fun ingestWazuh(
        @RequestBody alert: Map<String, Any?>,
    ): IngestResult = ingestionService.ingest(wazuhAlertMapper.toIngestRequest(alert))
}
