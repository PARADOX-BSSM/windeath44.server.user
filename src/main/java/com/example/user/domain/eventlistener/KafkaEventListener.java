package com.example.user.domain.eventlistener;

import com.chatbot.events.ChatEvent;
import com.example.user.avro.RemainTokenDecreaseResponse;
import com.example.user.avro.RemainTokenIncreaseResponse;
import com.example.user.domain.service.TokenDecreaseService;
import com.example.user.domain.service.TokenIncreaseService;
import com.example.user.global.infrastructure.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import windeath44.server.memorial.avro.MemorialBowedAvroSchema;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private final TokenDecreaseService tokenDecreaseService;
    private final TokenIncreaseService tokenIncreaseService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "remain-token-decrease-request", groupId = "user")
    @Transactional
    public void handleTokenDecreaseRequest(ChatEvent request) {
        log.info("토큰 감소 요청 수신 - userId: {}, tokenCount: {}", request.getUserId(), request.getTotalTokenCount());

        try {
            Long remainingToken = tokenDecreaseService.decreaseToken(
                    request.getUserId(),
                    request.getTotalTokenCount()
            );

            RemainTokenDecreaseResponse response = buildResponse(request.getUserId(), true, remainingToken, null);
            kafkaProducer.send("remain-token-decrease-response", response);
            
            log.info("토큰 감소 성공 - userId: {}, remainingToken: {}", request.getUserId(), remainingToken);

        } catch (Exception e) {
            log.error("토큰 감소 실패 - userId: {}, error: {}", request.getUserId(), e.getMessage(), e);

            RemainTokenDecreaseResponse response = buildResponse(request.getUserId(), false, null, e.getMessage());
            kafkaProducer.send("remain-token-decrease-fail-response", response);
            
            log.info("토큰 감소 실패 응답 발송 완료 - userId: {}", request.getUserId());
        }
    }

    private static RemainTokenDecreaseResponse buildResponse(String userId, boolean success, Long remainingToken, String errorMessage) {
        return RemainTokenDecreaseResponse.newBuilder()
                .setUserId(userId)
                .setSuccess(success)
                .setRemainingToken(remainingToken)
                .setErrorMessage(errorMessage)
                .build();
    }

    @KafkaListener(topics = "remain-token-increase-request", groupId = "user")
    @Transactional
    public void handleTokenIncreaseRequest(MemorialBowedAvroSchema memorialBowedAvroSchema) {
        String userId = memorialBowedAvroSchema.getWriterId();
        log.info("토큰 증가 요청 수신 - userId: {}, tokenBowCount: {}", userId, memorialBowedAvroSchema.getMemorialBow());

        try {
            Long remainingToken = tokenIncreaseService.increaseToken(
                    userId,
                    1000L
            );

            RemainTokenIncreaseResponse response = buildIncreaseResponse(userId, true, remainingToken, null);
            kafkaProducer.send("remain-token-increase-response", response);
            
            log.info("토큰 증가 성공 - userId: {}, remainingToken: {}", userId, remainingToken);

        } catch (Exception e) {
            log.error("토큰 증가 실패 - userId: {}, error: {}", userId, e.getMessage(), e);

            RemainTokenIncreaseResponse response = buildIncreaseResponse(userId, false, null, e.getMessage());
            kafkaProducer.send("remain-token-increase-fail-response", response);
            
            log.info("토큰 증가 실패 응답 발송 완료 - userId: {}", userId);
        }
    }

    private static RemainTokenIncreaseResponse buildIncreaseResponse(String userId, boolean success, Long remainingToken, String errorMessage) {
        return RemainTokenIncreaseResponse.newBuilder()
                .setUserId(userId)
                .setSuccess(success)
                .setRemainingToken(remainingToken)
                .setErrorMessage(errorMessage)
                .build();
    }
}

