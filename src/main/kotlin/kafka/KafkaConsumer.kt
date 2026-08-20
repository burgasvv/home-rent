package org.burgas.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.KafkaConsumer
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
                println("${consumerRecord.topic()} :: ${consumerRecord.value()}")
            }
        }
    }
}