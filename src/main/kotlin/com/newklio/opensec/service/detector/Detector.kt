package com.newklio.opensec.service.detector

import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SessionSummary

/**
 * A pluggable detector (PRD §6). Detectors inspect a normalized event and/or a session summary and
 * emit zero or more scored [DetectionSignal]s. Foundation for the Phase 17 plugin system.
 */
interface Detector {
    val name: String

    fun onEvent(event: NormalizedEvent): List<DetectionSignal> = emptyList()

    fun onSession(session: SessionSummary): List<DetectionSignal> = emptyList()
}
