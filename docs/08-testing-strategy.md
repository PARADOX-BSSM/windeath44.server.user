# User Server PRD - 테스트 전략

## 1. 테스트 피라미드

```
       /\
      /  \
     /E2E \      <- 5-10% (End-to-End Tests)
    /______\
   /        \
  /Integration\ <- 20-30% (Integration Tests)
 /____________\
/              \
/  Unit Tests   \ <- 60-70% (Unit Tests)
/________________\
```

## 2. 단위 테스트 (Unit Tests)

### 2.1 Service Layer 테스트
```java
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GrpcClientService grpcClientService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 등록 성공 테스트")
    void registerUser_Success() {
        // given
        RegisterUserRequest request = RegisterUserRequest.builder()
            .userId("testuser")
            .email("test@example.com")
            .name("테스트사용자")
            .password("password123!")
            .build();

        User user = User.builder()
            .userId("testuser")
            .email("test@example.com")
            .name("테스트사용자")
            .password("encoded_password")
            .role(UserRole.USER)
            .remainToken(10000L)
            .build();

        when(grpcClientService.validateEmail(request.getEmail())).thenReturn(true);
        when(userRepository.existsByUserId(request.getUserId())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        assertDoesNotThrow(() -> userService.register(request));

        // then
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user.created"), any(UserCreatedEvent.class));
    }

    @Test
    @DisplayName("중복된 사용자 ID로 등록 시 예외 발생")
    void registerUser_DuplicateUserId_ThrowsException() {
        // given
        RegisterUserRequest request = RegisterUserRequest.builder()
            .userId("existinguser")
            .email("test@example.com")
            .name("테스트사용자")
            .password("password123!")
            .build();

        when(grpcClientService.validateEmail(request.getEmail())).thenReturn(true);
        when(userRepository.existsByUserId(request.getUserId())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
            .isInstanceOf(AlreadyExistsUserIdException.class)
            .hasMessage("이미 존재하는 사용자 ID입니다: existinguser");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("토큰 감소 성공 테스트")
    void decreaseToken_Success() {
        // given
        String userId = "testuser";
        int decreaseAmount = 100;

        User user = User.builder()
            .userId(userId)
            .remainToken(1000L)
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        userService.decreaseToken(userId, decreaseAmount);

        // then
        assertThat(user.getRemainToken()).isEqualTo(900L);
        verify(kafkaTemplate).send(eq("user.token.decrease"), any(TokenDecreaseEvent.class));
    }

    @Test
    @DisplayName("토큰 부족 시 예외 발생")
    void decreaseToken_InsufficientToken_ThrowsException() {
        // given
        String userId = "testuser";
        int decreaseAmount = 500;

        User user = User.builder()
            .userId(userId)
            .remainToken(100L)
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.decreaseToken(userId, decreaseAmount))
            .isInstanceOf(InsufficientRemainTokenException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}
```

### 2.2 Repository Layer 테스트
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일로 사용자 조회 테스트")
    void findByEmail_Success() {
        // given
        User user = User.builder()
            .userId("testuser")
            .email("test@example.com")
            .name("테스트사용자")
            .password("encoded_password")
            .role(UserRole.USER)
            .remainToken(10000L)
            .build();

        entityManager.persistAndFlush(user);

        // when
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("사용자 ID 존재 여부 확인 테스트")
    void existsByUserId_True() {
        // given
        User user = User.builder()
            .userId("testuser")
            .email("test@example.com")
            .name("테스트사용자")
            .password("encoded_password")
            .role(UserRole.USER)
            .remainToken(10000L)
            .build();

        entityManager.persistAndFlush(user);

        // when
        boolean exists = userRepository.existsByUserId("testuser");

        // then
        assertThat(exists).isTrue();
    }
}
```

## 3. 통합 테스트 (Integration Tests)

### 3.1 Controller Integration Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    @DisplayName("사용자 등록 API 통합 테스트")
    void registerUser_Integration_Success() {
        // given
        RegisterUserRequest request = RegisterUserRequest.builder()
            .userId("integrationuser")
            .email("integration@example.com")
            .name("통합테스트사용자")
            .password("password123!")
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(request, headers);

        // when
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/users/register", entity, ApiResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo("success");

        // 데이터베이스 확인
        Optional<User> savedUser = userRepository.findByEmail("integration@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUserId()).isEqualTo("integrationuser");
    }

    @Test
    @DisplayName("사용자 프로필 조회 API 통합 테스트")
    void getUserProfile_Integration_Success() {
        // given
        User user = User.builder()
            .userId("profileuser")
            .email("profile@example.com")
            .name("프로필테스트사용자")
            .password("encoded_password")
            .role(UserRole.USER)
            .remainToken(10000L)
            .build();

        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", "profileuser");

        // when
        ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(
            "/users/profile",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getUserId()).isEqualTo("profileuser");
        assertThat(response.getBody().getData().getEmail()).isEqualTo("profile@example.com");
    }

    @Test
    @DisplayName("권한 없는 사용자 프로필 조회 시 403 에러")
    void getUserProfile_Unauthorized_Returns403() {
        // given
        User user = User.builder()
            .userId("privateuser")
            .email("private@example.com")
            .name("비공개사용자")
            .password("encoded_password")
            .role(UserRole.USER)
            .remainToken(10000L)
            .build();

        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", "anotheruser"); // 다른 사용자 ID

        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/users/profile",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ErrorResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```

### 3.2 gRPC Integration Test
```java
@SpringBootTest
@Testcontainers
class GrpcUserServiceIntegrationTest {

    @Container
    static GenericContainer<?> grpcServer = new GenericContainer<>("grpc-test-server:latest")
        .withExposedPorts(9090);

    @Autowired
    private GrpcUserServiceClient grpcUserServiceClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("grpc.client.user-service.address",
            () -> "dns:///" + grpcServer.getHost() + ":" + grpcServer.getMappedPort(9090));
    }

    @Test
    @DisplayName("gRPC 사용자 조회 통합 테스트")
    void getUser_GrpcIntegration_Success() {
        // given
        String userId = "grpcuser";

        // when
        UserResponse response = grpcUserServiceClient.getUser(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getEmail()).isNotEmpty();
    }

    @Test
    @DisplayName("gRPC 로그인 통합 테스트")
    void loginUser_GrpcIntegration_Success() {
        // given
        LoginRequest request = LoginRequest.newBuilder()
            .setEmail("grpc@example.com")
            .setPassword("password123!")
            .build();

        // when
        LoginResponse response = grpcUserServiceClient.loginUser(request);

        // then
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getToken()).isNotEmpty();
    }
}
```

### 3.3 Kafka Integration Test
```java
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"user.token.increase", "user.token.decrease"})
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TokenEventService tokenEventService;

    @Test
    @DisplayName("토큰 증가 이벤트 발행 테스트")
    void publishTokenIncreaseEvent_Success() throws Exception {
        // given
        String userId = "kafkauser";
        long tokenCount = 1000L;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TokenIncreaseEvent> receivedEvent = new AtomicReference<>();

        // Kafka Consumer 설정
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, TokenIncreaseEvent> consumerFactory =
            new DefaultKafkaConsumerFactory<>(consumerProps);
        KafkaConsumer<String, TokenIncreaseEvent> consumer = consumerFactory.createConsumer();

        consumer.subscribe(Collections.singletonList("user.token.increase"));

        // Consumer 스레드에서 메시지 수신
        CompletableFuture.runAsync(() -> {
            ConsumerRecords<String, TokenIncreaseEvent> records = consumer.poll(Duration.ofSeconds(10));
            for (ConsumerRecord<String, TokenIncreaseEvent> record : records) {
                receivedEvent.set(record.value());
                latch.countDown();
            }
            consumer.close();
        });

        // when
        tokenEventService.publishTokenIncreaseEvent(userId, tokenCount).join();

        // then
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedEvent.get()).isNotNull();
        assertThat(receivedEvent.get().getUserId()).isEqualTo(userId);
        assertThat(receivedEvent.get().getTokenCount()).isEqualTo(tokenCount);
    }
}
```

## 4. End-to-End 테스트

### 4.1 API E2E 테스트
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class UserServiceE2ETest {

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
        new File("src/test/resources/docker-compose-test.yml"))
        .withExposedService("user-service", 8080)
        .withExposedService("mysql", 3306)
        .withExposedService("kafka", 9092);

    private static final String BASE_URL = "http://localhost:8080";

    @Test
    @DisplayName("사용자 전체 워크플로우 E2E 테스트")
    void userWorkflow_E2E_Success() {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 사용자 등록
        RegisterUserRequest registerRequest = RegisterUserRequest.builder()
            .userId("e2euser")
            .email("e2e@example.com")
            .name("E2E테스트사용자")
            .password("password123!")
            .build();

        ResponseEntity<ApiResponse> registerResponse = restTemplate.postForEntity(
            BASE_URL + "/users/register", registerRequest, ApiResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. 사용자 프로필 조회
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", "e2euser");

        ResponseEntity<ApiResponse<UserResponse>> profileResponse = restTemplate.exchange(
            BASE_URL + "/users/profile",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody().getData().getUserId()).isEqualTo("e2euser");

        // 3. 사용자 이름 변경
        UpdateUserNameRequest nameRequest = UpdateUserNameRequest.builder()
            .name("변경된이름")
            .build();

        ResponseEntity<ApiResponse> nameResponse = restTemplate.exchange(
            BASE_URL + "/users/name",
            HttpMethod.PATCH,
            new HttpEntity<>(nameRequest, headers),
            ApiResponse.class
        );

        assertThat(nameResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. 변경된 정보 확인
        ResponseEntity<ApiResponse<UserResponse>> updatedProfileResponse = restTemplate.exchange(
            BASE_URL + "/users/profile",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(updatedProfileResponse.getBody().getData().getName()).isEqualTo("변경된이름");

        // 5. 사용자 삭제
        ResponseEntity<ApiResponse> deleteResponse = restTemplate.exchange(
            BASE_URL + "/users",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            ApiResponse.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 6. 삭제 확인
        ResponseEntity<ErrorResponse> notFoundResponse = restTemplate.exchange(
            BASE_URL + "/users/profile",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ErrorResponse.class
        );

        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

## 5. 성능 테스트

### 5.1 부하 테스트
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class UserServicePerformanceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private final ExecutorService executor = Executors.newFixedThreadPool(50);

    @Test
    @DisplayName("사용자 조회 성능 테스트")
    void getUserProfile_PerformanceTest() throws Exception {
        // given
        RestTemplate restTemplate = new RestTemplate();
        int numberOfRequests = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < numberOfRequests; i++) {
            executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("user-id", "perfuser");

                    ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(
                        BASE_URL + "/users/profile",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
                    );

                    long endTime = System.currentTimeMillis();
                    responseTimes.add(endTime - startTime);

                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                } finally {
                    latch.countDown();
                }
            });
        }

        // then
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

        long p95ResponseTime = responseTimes.stream()
            .sorted()
            .skip((long) (responseTimes.size() * 0.95))
            .findFirst()
            .orElse(0L);

        System.out.printf("평균 응답시간: %.2f ms%n", averageResponseTime);
        System.out.printf("P95 응답시간: %d ms%n", p95ResponseTime);

        // 성능 기준 검증
        assertThat(averageResponseTime).isLessThan(100.0); // 평균 100ms 이하
        assertThat(p95ResponseTime).isLessThan(200L);      // P95 200ms 이하
    }
}
```

## 6. 테스트 설정 및 유틸리티

### 6.1 테스트 프로파일 설정
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: password
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect

  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}

logging:
  level:
    io.windeath44: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

### 6.2 테스트 데이터 빌더
```java
public class UserTestDataBuilder {

    public static User.UserBuilder defaultUser() {
        return User.builder()
            .userId("testuser")
            .email("test@example.com")
            .name("테스트사용자")
            .password("encoded_password")
            .role(UserRole.USER)
            .remainToken(10000L)
            .createdAt(LocalDateTime.now());
    }

    public static User.UserBuilder adminUser() {
        return defaultUser()
            .userId("adminuser")
            .email("admin@example.com")
            .name("관리자")
            .role(UserRole.ADMIN);
    }

    public static RegisterUserRequest.RegisterUserRequestBuilder defaultRegisterRequest() {
        return RegisterUserRequest.builder()
            .userId("testuser")
            .email("test@example.com")
            .name("테스트사용자")
            .password("password123!");
    }
}
```

### 6.3 테스트 유틸리티
```java
@TestComponent
public class TestUtils {

    public static void clearDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    public static String generateRandomEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    public static String generateRandomUserId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static HttpHeaders createAuthHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

## 7. 테스트 자동화

### 7.1 GitHub Actions 테스트 워크플로우
```yaml
# .github/workflows/tests.yml
name: Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Run unit tests
      run: ./gradlew test

    - name: Generate test report
      run: ./gradlew jacocoTestReport

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3

  integration-tests:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: password
          MYSQL_DATABASE: testdb
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Run integration tests
      run: ./gradlew integrationTest
      env:
        DB_URL: jdbc:mysql://localhost:3306/testdb
        DB_USERNAME: root
        DB_PASSWORD: password

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Start test environment
      run: docker-compose -f docker-compose-test.yml up -d

    - name: Wait for services
      run: ./scripts/wait-for-services.sh

    - name: Run E2E tests
      run: ./gradlew e2eTest

    - name: Stop test environment
      run: docker-compose -f docker-compose-test.yml down
```

### 7.2 테스트 커버리지 설정
```groovy
// build.gradle
jacoco {
    toolVersion = "0.8.8"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/config/**',
                '**/dto/**',
                '**/exception/**',
                '**/*Application.class'
            ])
        }))
    }
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport

    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

## 8. 테스트 모니터링

### 8.1 테스트 메트릭 수집
```java
@TestExecutionListener
public class TestMetricsListener implements TestExecutionListener {

    private static final MeterRegistry meterRegistry = Metrics.globalRegistry;
    private static final Timer testExecutionTimer = Timer.builder("test.execution.time")
        .description("Test execution time")
        .register(meterRegistry);

    private final Map<String, Timer.Sample> samples = new ConcurrentHashMap<>();

    @Override
    public void testExecutionStarted(TestIdentifier testIdentifier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        samples.put(testIdentifier.getUniqueId(), sample);
    }

    @Override
    public void testExecutionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        Timer.Sample sample = samples.remove(testIdentifier.getUniqueId());
        if (sample != null) {
            sample.stop(testExecutionTimer.tag("test", testIdentifier.getDisplayName())
                .tag("status", testExecutionResult.getStatus().toString()));
        }
    }
}
```