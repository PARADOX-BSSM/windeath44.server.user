package com.example.user.domain.service;

import com.example.user.domain.exception.NotFoundUserException;
import com.example.user.domain.model.User;
import com.example.user.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class XpIncreaseService {
    private final UserRepository userRepository;

    @Transactional
    public void applyXpIncrease(String userId, Long addedXp, Long totalXp) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(NotFoundUserException::getInstance);

        user.applyXpIncrease(addedXp, totalXp);
    }
}
