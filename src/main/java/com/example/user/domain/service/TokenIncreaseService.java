package com.example.user.domain.service;

import com.example.user.domain.exception.NotFoundUserException;
import com.example.user.domain.model.User;
import com.example.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenIncreaseService {
    private final UserRepository userRepository;

    @Transactional
    public Long increaseToken(String userId, Long tokenCount) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(NotFoundUserException::getInstance);

        user.increaseToken(tokenCount);
        userRepository.save(user);

        log.info("토큰 증가 완료 - userId: {}, 증가량: {}, 남은 토큰: {}", userId, tokenCount, user.getRemainToken());

        return user.getRemainToken();
    }
}
