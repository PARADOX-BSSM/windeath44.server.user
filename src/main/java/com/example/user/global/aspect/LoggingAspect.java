package com.example.user.global.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.example.user.domain.service.*.*(..))")
    public void userServiceMethodLogging() {}

    @Around("userServiceMethodLogging()")
    public Object serviceMethodLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        log.trace("Entering {}", joinPoint.getSignature().getName());
        Object result = joinPoint.proceed();
        log.trace("Exiting {}", joinPoint.getSignature().getName());
        return result;
    }
}
