package com.newklio.opensec.service

import com.newklio.opensec.dto.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Default event publisher used when the Kafka event bus is disabled (`opensec.kafka.enabled` unset
 * or false). Lets the app run locally and in tests without a broker. Swapped for
 * [KafkaEventPublisher] when Kafka is enabled.
 */
@Service
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "false", matchIfMissing = true)
class NoopEventPublisher : EventPublisher {
    private val log = LoggerFactory.getLogger(NoopEventPublisher::class.java)

    override fun publish(
        topic: String,
        envelope: EventEnvelope,
    ) {
        log.debug("Event bus disabled; skipping publish of {} to topic {}", envelope.eventType, topic)
    }
}
