# User Server PRD - 보안 및 예외 처리

## 1. 보안 정책

### 1.1 인증 및 인가

#### 1.1.1 현재 보안 설정
- 현재는 모든 요청 허용 (`.anyRequest().permitAll()`)
- 향후 외부 인증 서비스와 연동 예정

#### 1.1.2 Role 기반 접근 제어 (RBAC)
- UserRole enum으로 USER, ADMIN 구분
- 서비스 레이어에서 권한 검증 로직 구현

#### 1.1.3 권한 검증
- 관리자만 다른 관리자 등록 가능
- 본인 정보만 수정 가능 (관리자 제외)
- user-id 헤더를 통한 사용자 식별

### 1.2 데이터 보호

#### 1.2.1 비밀번호 정책
```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
- BCrypt 암호화 적용
- 평문 비밀번호 저장 금지

#### 1.2.2 SQL Injection 방지
- JPA/Hibernate 사용으로 자동 방지
- Parameter Binding으로 안전한 쿼리 실행

### 1.3 XSS 방지

#### 1.3.1 입력값 필터링
- XssStringJsonDeserializer를 통한 XSS 공격 방지
- script 태그 필터링 적용
- Jackson 커스텀 Deserializer 사용

```java
@Component
public class XssStringJsonDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) return null;
        return sanitize(value);
    }

    private String sanitize(String value) {
        return value
                .replaceAll("(?i)<script", "&lt;script")
                .replaceAll("(?i)</script>", "&lt;/script&gt;");
    }
}
```

## 2. 예외 처리 시스템

### 2.1 비즈니스 예외 정의

#### 2.1.1 현재 구현된 예외 클래스들
- **NotFoundUserException**: 사용자를 찾을 수 없을 때
- **AlreadyExistsUserIdException**: 중복된 사용자 ID
- **AlreadyExistsUserEmailException**: 중복된 이메일
- **ValidationPasswordException**: 비밀번호 검증 실패
- **NotAdminException**: 관리자 권한 부족
- **InsufficientRemainTokenException**: 토큰 부족
- **FailedUploadFileException**: 파일 업로드 실패

모든 예외는 GlobalException을 상속하며 ErrorCode enum을 사용합니다.

### 2.2 에러 코드 정의
```java
@AllArgsConstructor
@Getter
public enum ErrorCode {
    USER_ID_ALREADY_EXISTS(400, "User Id already exists"),
    USER_EMAIL_ALREADY_EXISTS(400, "User email already exists"),
    USER_NOT_FOUND(404, "User not found"),
    PASSWORD_VALIDATION_FAILED(400, "Password validation failed"),
    FILE_UPLOAD_FAILED(500, "File upload failed"),
    REMAIN_TOKEN_INSUFFICIENT(500, "Insufficient token balance"),
    NOT_ADMIN(403, "Admin permission required");

    private int status;
    private String message;
}
```

### 2.3 전역 예외 처리
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> globalException(GlobalException e) {
        ErrorCode errorCode = e.getErrorCode();
        int status = errorCode.getStatus();
        log.error(errorCode.getMessage());

        return new ResponseEntity<>(new ErrorResponse(errorCode), HttpStatus.valueOf(status));
    }

    @ExceptionHandler(GrpcMappedException.class)
    public ResponseEntity<Void> grpcMappedException(GrpcMappedException e) {
        return ResponseEntity
                .status(e.getStatus())
                .build();
    }
}
```

## 3. 보안 개선 계획 (향후)

### 3.1 추가 예정 보안 기능
- JWT 토큰 기반 인증 도입
- HTTPS 통신 강제
- gRPC TLS 암호화
- API Rate Limiting
- 2단계 인증 (2FA)

### 3.2 보안 모니터링 강화
- 보안 이벤트 로깅
- 무권한 접근 감지
- 실패한 로그인 시도 추적
- 실시간 알람 시스템