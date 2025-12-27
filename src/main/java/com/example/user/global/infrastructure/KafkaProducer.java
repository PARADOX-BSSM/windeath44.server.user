package com.example.user.global.infrastructure;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    public void send(String topic, SpecificRecord message) {
        kafkaTemplate.send(topic, message);
    }

    public void send(String topic, SpecificRecord message, Map<String, Object> headers) {
        Message<SpecificRecord> kafkaMessage = MessageBuilder
                .withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .copyHeaders(headers)
                .build();
        kafkaTemplate.send(kafkaMessage);
    }
}
