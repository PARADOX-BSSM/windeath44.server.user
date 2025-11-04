package com.example.user.global.infrastructure;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    public void send(String topic, SpecificRecord message) {
        kafkaTemplate.send(topic, message);
    }
}
