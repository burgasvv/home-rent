package org.burgas.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.burgas.dto.IdentityResponse
import java.time.Duration

fun CoroutineScope.configureKafkaConsumer() {

    launch(Dispatchers.IO) {
        val kafkaConsumer = KafkaConsumer<String, String>(KafkaConfig.getConsumerProps())
        kafkaConsumer.subscribe(
            listOf(
                KafkaTopics.identityTopic.name()
            )
        )
        while (isActive) {
            val consumerRecords = kafkaConsumer.poll(Duration.ofMillis(10))
            consumerRecords.forEach { consumerRecord ->
                when(consumerRecord.topic()) {
                    KafkaTopics.identityTopic.name() -> {
                        val identityResponse = Json.decodeFromString<IdentityResponse>(consumerRecord.value())
                        println("${consumerRecord.topic()} :: ${consumerRecord.key()} :: $identityResponse")
                    }
                    else -> println("Wrong topic :: ${consumerRecord.topic()}")
                }
            }
        }
    }
}