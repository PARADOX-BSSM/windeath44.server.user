package com.example.user.domain.eventlistener;

import windeath44.server.chatbot.avro.ChatAvroSchema;
import com.example.user.domain.service.TokenDecreaseService;
import com.example.user.domain.service.TokenIncreaseService;
import com.example.user.domain.service.XpIncreaseService;
import com.example.user.global.infrastructure.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import windeath44.server.memorial.avro.MemorialBowedAvroSchema;
import windeath44.server.user.avro.RemainTokenDecreaseResponse;
import windeath44.server.user.avro.RemainTokenIncreaseResponse;
import windeath44.server.user.avro.XpIncreaseResponse;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private final TokenDecreaseService tokenDecreaseService;
    private final TokenIncreaseService tokenIncreaseService;
    private final XpIncreaseService xpIncreaseService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "remain-token-decrease-request", groupId = "user")
    @Transactional
    public void handleTokenDecreaseRequest(ChatAvroSchema request) {
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

    @KafkaListener(topics = "xp-increase-response", groupId = "user")
    @Transactional
    public void handleXpIncreaseResponse(
            XpIncreaseResponse response,
            @Header(value = "processedByUserService", required = false) Boolean processed
    ) {
        if (Boolean.TRUE.equals(processed)) {
            log.debug("XP 증가 응답 이미 처리됨 - userId: {}", response.getUserId());
            return;
        }

        log.info("XP 증가 응답 수신 - userId: {}, success: {}, addedXp: {}, totalXp: {}",
                response.getUserId(),
                response.getSuccess(),
                response.getAddedXp(),
                response.getTotalXp());

        if (!response.getSuccess()) {
            log.warn("XP 증가 실패 응답 - userId: {}", response.getUserId());
            return;
        }

        try {
            xpIncreaseService.applyXpIncrease(
                    response.getUserId(),
                    response.getAddedXp(),
                    response.getTotalXp()
            );
            log.info("XP 업데이트 완료 - userId: {}", response.getUserId());

            kafkaProducer.send(
                    "xp-increase-response",
                    response,
                    Map.of("processedByUserService", true)
            );
            log.info("XP 증가 응답 발행 완료 - userId: {}", response.getUserId());
        } catch (Exception e) {
            log.error("XP 업데이트 처리 실패 - userId: {}, error: {}", response.getUserId(), e.getMessage(), e);
        }
    }
}
