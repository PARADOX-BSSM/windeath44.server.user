package com.example.user.domain.eventlistener;

import com.chatbot.events.ChatEvent;
import com.example.user.avro.RemainTokenDecreaseResponse;
import com.example.user.domain.service.TokenDecreaseService;
import com.example.user.global.infrastructure.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private final TokenDecreaseService tokenDecreaseService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "remain-token-decrease-request", groupId = "user")
    @Transactional
    public void handleTokenDecreaseRequest(ChatEvent request) {
        log.info("토큰 감소 요청 수신 - userId: {}, tokenCount: {}", request.getUserId(), request.getTotalTokenCount());

        RemainTokenDecreaseResponse response;

        try {
            Long remainingToken = tokenDecreaseService.decreaseToken(
                    request.getUserId(),
                    request.getTotalTokenCount()
            );

            response = getBuild(request, true, remainingToken, null);

            log.info("토큰 감소 성공 - userId: {}, remainingToken: {}", request.getUserId(), remainingToken);

        } catch (Exception e) {
            log.error("토큰 감소 실패 - userId: {}, error: {}", request.getUserId(), e.getMessage(), e);

            response = getBuild(request, false, null, e.getMessage());
        }

        kafkaProducer.send("remain-token-decrease-response", response);
        log.info("토큰 감소 응답 발송 완료 - userId: {}, success: {}", request.getUserId(), response.getSuccess());
    }

    private static RemainTokenDecreaseResponse getBuild(ChatEvent event, boolean success, Long remainingToken, String errorMessage) {
        return RemainTokenDecreaseResponse.newBuilder()
                .setUserId(event.getUserId())
                .setSuccess(success)
                .setRemainingToken(remainingToken)
                .setErrorMessage(errorMessage)
                .build();
    }
}

