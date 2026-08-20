package org.burgas.kafka

import org.apache.kafka.clients.admin.NewTopic

object KafkaTopics {

    val identityTopic = NewTopic("identity-topic", 5, 5)
}