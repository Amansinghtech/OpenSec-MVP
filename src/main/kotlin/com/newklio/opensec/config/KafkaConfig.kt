package com.newklio.opensec.config

import com.newklio.opensec.service.EventPublisher
import com.newklio.opensec.service.KafkaEventPublisher
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import tools.jackson.databind.ObjectMapper

/**
 * Kafka event-bus wiring. Only active when `opensec.kafka.enabled=true`, so the app and tests run
 * without a broker by default. Topics are created with multiple partitions (partitioned by tenant).
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class KafkaConfig(
    @param:Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,
    @param:Value("\${opensec.kafka.partitions:3}")
    private val partitions: Int,
    @param:Value("\${opensec.kafka.consumer-group:opensec}")
    private val consumerGroup: String,
) {
    // --- Producer side ---

    @Bean
    fun eventProducerFactory(): ProducerFactory<String, String> {
        val configs =
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            )
        return DefaultKafkaProducerFactory(configs)
    }

    @Bean
    fun kafkaTemplate(eventProducerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        KafkaTemplate(eventProducerFactory)

    @Bean
    fun kafkaEventPublisher(
        kafkaTemplate: KafkaTemplate<String, String>,
        objectMapper: ObjectMapper,
    ): EventPublisher = KafkaEventPublisher(kafkaTemplate, objectMapper)

    // --- Consumer side ---

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val configs =
            mapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to consumerGroup,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            )
        return DefaultKafkaConsumerFactory(configs)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(consumerFactory)
        return factory
    }

    // --- Topics ---

    @Bean
    fun rawLogsTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.RAW_LOGS)
            .partitions(partitions)
            .replicas(1)
            .build()

    @Bean
    fun normalizedEventsTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.NORMALIZED_EVENTS)
            .partitions(partitions)
            .replicas(1)
            .build()

    @Bean
    fun sessionEventsTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.SESSION_EVENTS)
            .partitions(partitions)
            .replicas(1)
            .build()

    @Bean
    fun detectionSignalsTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.DETECTION_SIGNALS)
            .partitions(partitions)
            .replicas(1)
            .build()

    @Bean
    fun contextualizedDetectionsTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.CONTEXTUALIZED_DETECTIONS)
            .partitions(partitions)
            .replicas(1)
            .build()
}
