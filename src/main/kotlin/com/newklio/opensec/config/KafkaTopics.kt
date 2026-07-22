package com.newklio.opensec.config

/**
 * Canonical Kafka topic names for the event bus (PRD §Event Bus). All topics are partitioned by
 * `tenant_id` (used as the record key) so a tenant's events stay ordered and processing scales
 * horizontally.
 */
object KafkaTopics {
    const val RAW_LOGS = "raw_logs"
    const val NORMALIZED_EVENTS = "normalized_events"
    const val SESSION_EVENTS = "session_events"
    const val DETECTION_SIGNALS = "detection_signals"
    const val CONTEXTUALIZED_DETECTIONS = "contextualized_detections"
    const val ALERT_EVENTS = "alert_events"
    const val DEFENSE_EVENTS = "defense_events"

    val ALL =
        listOf(
            RAW_LOGS,
            NORMALIZED_EVENTS,
            SESSION_EVENTS,
            DETECTION_SIGNALS,
            CONTEXTUALIZED_DETECTIONS,
            ALERT_EVENTS,
            DEFENSE_EVENTS,
        )
}
