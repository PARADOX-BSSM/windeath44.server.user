# User Server PRD - 외부 시스템 연동

## 1. gRPC 서비스 연동

### 1.1 인증 서비스 연동
```proto
service AuthService {
    rpc ValidateEmail(EmailRequest) returns (EmailResponse);
}

message EmailRequest {
    string email = 1;
}

message EmailResponse {
    bool isValid = 1;
    string errorMessage = 2;
}
```

### 1.2 사용자 조회 서비스
```proto
service UserService {
    rpc GetUser(UserRequest) returns (UserResponse);
    rpc LoginUser(LoginRequest) returns (LoginResponse);
}

message UserRequest {
    string userId = 1;
}

message UserResponse {
    string userId = 1;
    string email = 2;
    string name = 3;
    string role = 4;
    int64 remainToken = 5;
    string profile = 6;
    string createdAt = 7;
}

message LoginRequest {
    string email = 1;
    string password = 2;
}

message LoginResponse {
    bool success = 1;
    string token = 2;
    UserResponse user = 3;
}
```

### 1.3 gRPC 클라이언트 구성
```java
@Service
public class GrpcClientService {

    @Autowired
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    @Autowired
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public boolean validateEmail(String email) {
        EmailRequest request = EmailRequest.newBuilder()
            .setEmail(email)
            .build();

        EmailResponse response = authServiceStub.validateEmail(request);
        return response.getIsValid();
    }

    public UserResponse getUser(String userId) {
        UserRequest request = UserRequest.newBuilder()
            .setUserId(userId)
            .build();

        return userServiceStub.getUser(request);
    }
}
```

## 2. Apache Kafka 메시징

### 2.1 토큰 관련 이벤트

#### 2.1.1 토큰 증가 이벤트
**Topic**: `user.token.increase`
```json
{
    "schema": {
        "type": "record",
        "name": "TokenIncreaseEvent",
        "fields": [
            {"name": "userId", "type": "string"},
            {"name": "tokenCount", "type": "long"},
            {"name": "timestamp", "type": "string"},
            {"name": "eventId", "type": "string"}
        ]
    },
    "payload": {
        "userId": "user123",
        "tokenCount": 1000,
        "timestamp": "2024-01-01T10:00:00Z",
        "eventId": "evt_123456"
    }
}
```

#### 2.1.2 토큰 감소 이벤트
**Topic**: `user.token.decrease`
```json
{
    "schema": {
        "type": "record",
        "name": "TokenDecreaseEvent",
        "fields": [
            {"name": "userId", "type": "string"},
            {"name": "tokenCount", "type": "int"},
            {"name": "timestamp", "type": "string"},
            {"name": "eventId", "type": "string"}
        ]
    },
    "payload": {
        "userId": "user123",
        "tokenCount": 100,
        "timestamp": "2024-01-01T10:00:00Z",
        "eventId": "evt_123457"
    }
}
```

### 2.2 사용자 생성/수정 이벤트

#### 2.2.1 사용자 생성 이벤트
**Topic**: `user.created`
```json
{
    "schema": {
        "type": "record",
        "name": "UserCreatedEvent",
        "fields": [
            {"name": "userId", "type": "string"},
            {"name": "email", "type": "string"},
            {"name": "name", "type": "string"},
            {"name": "role", "type": "string"},
            {"name": "timestamp", "type": "string"}
        ]
    },
    "payload": {
        "userId": "user123",
        "email": "user@example.com",
        "name": "사용자",
        "role": "USER",
        "timestamp": "2024-01-01T10:00:00Z"
    }
}
```

#### 2.2.2 사용자 삭제 이벤트
**Topic**: `user.deleted`
```json
{
    "schema": {
        "type": "record",
        "name": "UserDeletedEvent",
        "fields": [
            {"name": "userId", "type": "string"},
            {"name": "timestamp", "type": "string"}
        ]
    },
    "payload": {
        "userId": "user123",
        "timestamp": "2024-01-01T10:00:00Z"
    }
}
```

### 2.3 Kafka 프로듀서/컨슈머 설정

#### 2.3.1 프로듀서 설정
```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "${kafka.bootstrap-servers}");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

#### 2.3.2 컨슈머 설정
```java
@Service
public class TokenEventConsumer {

    @KafkaListener(topics = "user.token.increase")
    public void handleTokenIncrease(TokenIncreaseEvent event) {
        // 토큰 증가 이벤트 처리
        userService.increaseToken(event.getUserId(), event.getTokenCount());
    }

    @KafkaListener(topics = "user.token.decrease")
    public void handleTokenDecrease(TokenDecreaseEvent event) {
        // 토큰 감소 이벤트 처리 (로깅, 통계 등)
        analyticsService.recordTokenUsage(event.getUserId(), event.getTokenCount());
    }
}
```

## 3. AWS S3 연동

### 3.1 S3 버킷 구성
```yaml
# S3 버킷 구조
user-profile-images/
  ├── original/
  │   └── {userId}/
  │       └── profile.{ext}
  ├── thumbnails/
  │   └── {userId}/
  │       ├── small_profile.jpg
  │       ├── medium_profile.jpg
  │       └── large_profile.jpg
  └── temp/
      └── {uploadId}/
          └── temp_file.{ext}
```

### 3.2 S3 서비스 구현
```java
@Service
public class S3Service {

    private final AmazonS3 s3Client;
    private final String bucketName;

    public String uploadProfileImage(MultipartFile file, String userId) {
        try {
            // 파일 검증
            validateFile(file);

            // 파일명 생성
            String fileName = generateFileName(userId, file.getOriginalFilename());

            // S3 업로드
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            PutObjectRequest request = new PutObjectRequest(
                bucketName,
                "original/" + userId + "/" + fileName,
                file.getInputStream(),
                metadata
            );

            s3Client.putObject(request);

            // CDN URL 반환
            return generateCdnUrl(userId, fileName);

        } catch (Exception e) {
            throw new FailedUploadFileException("파일 업로드에 실패했습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        // 파일 크기 검증 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidFileSizeException("파일 크기가 5MB를 초과합니다.");
        }

        // 파일 형식 검증
        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new InvalidFileTypeException("지원하지 않는 파일 형식입니다.");
        }
    }
}
```

### 3.3 S3 보안 정책
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::user-profile-images/original/*"
        },
        {
            "Sid": "UserServiceAccess",
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::ACCOUNT:role/UserServiceRole"
            },
            "Action": [
                "s3:PutObject",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::user-profile-images/*"
        }
    ]
}
```

## 4. 외부 시스템 연동 모니터링

### 4.1 gRPC 연결 상태 모니터링
```java
@Component
public class GrpcHealthChecker {

    @Scheduled(fixedDelay = 30000) // 30초마다 실행
    public void checkGrpcServices() {
        // Auth Service 상태 확인
        checkAuthServiceHealth();

        // User Service 상태 확인
        checkUserServiceHealth();
    }

    private void checkAuthServiceHealth() {
        try {
            authServiceStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .validateEmail(EmailRequest.newBuilder()
                    .setEmail("health@check.com")
                    .build());
        } catch (Exception e) {
            logger.error("Auth Service 연결 실패", e);
            alertService.sendAlert("Auth Service Down", e.getMessage());
        }
    }
}
```

### 4.2 Kafka 연결 상태 모니터링
```java
@Component
public class KafkaHealthChecker {

    @EventListener
    public void handleKafkaConnectionLost(KafkaConnectionLostEvent event) {
        logger.error("Kafka 연결 끊김: {}", event.getReason());
        alertService.sendAlert("Kafka Connection Lost", event.getReason());
    }

    @EventListener
    public void handleKafkaConnectionRestored(KafkaConnectionRestoredEvent event) {
        logger.info("Kafka 연결 복구됨");
        alertService.sendAlert("Kafka Connection Restored", "연결이 복구되었습니다.");
    }
}
```

### 4.3 S3 연결 상태 모니터링
```java
@Component
public class S3HealthChecker {

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    public void checkS3Connection() {
        try {
            // S3 버킷 존재 확인
            boolean exists = s3Client.doesBucketExistV2(bucketName);
            if (!exists) {
                throw new RuntimeException("S3 버킷이 존재하지 않습니다.");
            }

            // 테스트 파일 업로드/삭제
            testS3Upload();

        } catch (Exception e) {
            logger.error("S3 연결 실패", e);
            alertService.sendAlert("S3 Connection Failed", e.getMessage());
        }
    }
}
```