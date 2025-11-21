# User Server PRD - 성능 및 모니터링

## 1. 현재 구현된 성능 설정

### 1.1 데이터베이스 설정
- **Primary Key**: userId (자동 생성)
- **Unique Constraint**: email (JPA @Column(unique = true))
- **JPA 설정**: hibernate.ddl-auto = update
- **Database Platform**: MySQL8Dialect

### 1.2 JPA Repository 구현
```java
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
}
```

### 1.3 현재 애플리케이션 설정
```yaml
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.MySQL8Dialect

  kafka:
    bootstrap-servers: ${KAFKA_URL}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer

server:
  port: ${SERVER_PORT}

logging:
  level:
    com.example.user: trace
```

## 2. 모니터링 설정

### 2.1 로깅 설정
- **로그 레벨**: trace (개발 환경)
- **SQL 로깅**: show-sql = true
- **패키지별 로깅**: com.example.user 패키지 trace 레벨

### 2.2 현재 구현된 예외 로깅
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
}
```

## 3. 외부 시스템 연동 현황

### 3.1 gRPC 설정
```yaml
grpc:
  server:
    address: ${GRPC_SERVER_ADDRESS}
    port: ${GRPC_SERVER_PORT}
  client:
    auth-server:
      address: ${GRPC_AUTH_SERVER_ADDRESS}
      negotiationType: "plaintext"
```

### 3.2 Kafka 설정
```yaml
kafka:
  consumer:
    group-id: user
    auto-offset-reset: earliest
    enable-auto-commit: false
  producer:
    key-serializer: StringSerializer
    value-serializer: KafkaAvroSerializer
```

### 3.3 S3 스토리지 설정
```yaml
storage:
  access-key: ${STORAGE_ACCESS_KEY}
  secret-key: ${STORAGE_SECRET_KEY}
  bucket-name: ${STORAGE_BUCKET_NAME}
  region: ap-northeast-2
```

## 4. 현재 성능 특성

### 4.1 User Entity 특성
- **토큰 기본값**: 10,000개
- **기본 프로필 이미지**: S3 URL
- **비밀번호 암호화**: BCrypt
- **감사 기능**: @CreatedDate 적용

### 4.2 토큰 관리 로직
```java
public void decreaseToken(int tokenCount) {
    boolean canDecreaseRemainToken = this.remainToken >= tokenCount;
    if (!canDecreaseRemainToken)
        throw InsufficientRemainTokenException.getInstance();
    this.remainToken -= tokenCount;
}

public void increaseToken(Long tokenCount) {
    this.remainToken += tokenCount;
}
```