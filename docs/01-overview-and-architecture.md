# User Server PRD - 개요 및 아키텍처

## 1. 개요 (Overview)

### 1.1 제품 소개
**최애의 사인** User Server는 마이크로서비스 아키텍처 기반의 사용자 관리 서비스입니다. Spring Boot 3.2.5와 Java 17을 기반으로 구축되었으며, 사용자 등록, 인증, 프로필 관리, 권한 관리 등의 핵심 기능을 제공합니다.

### 1.2 프로젝트 정보
- **프로젝트명**: 최애의 사인 (User Management Service)
- **기술 스택**: Spring Boot 3.2.5, Java 17, JPA/Hibernate, MySQL, gRPC, Kafka, AWS S3
- **아키텍처**: Microservice Architecture
- **데이터베이스**: MySQL
- **메시징**: Apache Kafka with Avro Schema
- **파일 스토리지**: AWS S3
- **통신 프로토콜**: REST API, gRPC

### 1.3 목적 및 비전
애니메이션 캐릭터 추모 서비스의 사용자 관리 백본 역할을 수행하며, 확장 가능하고 안전한 사용자 관리 기능을 제공합니다.

### 1.4 커밋 컨벤션
- 형식: `행동 :: 내용 [스크럼 키]`
- 스크럼 키는 현재 작업 브랜치 이름의 마지막 세그먼트에서 가져온다. 예: 브랜치 `feat-0.3.2/admin/PW-382` → 키 `PW-382`.
- 예시 커밋 메시지: `feat :: add role change API [PW-382]`

## 2. 시스템 아키텍처 (System Architecture)

### 2.1 기술 스택
```
┌─────────────────────────────────────────────────────────┐
│                  Presentation Layer                      │
├─────────────────────────────────────────────────────────┤
│ REST API Controllers                                    │
│ - UserController                                        │
│ - RegisterController                                    │
│ - RetrieveUserController                               │
├─────────────────────────────────────────────────────────┤
│                  Business Logic Layer                   │
├─────────────────────────────────────────────────────────┤
│ Services                                                │
│ - UserService                                           │
│ - TokenIncreaseService                                  │
│ - TokenDecreaseService                                  │
│ - GrpcClientService                                     │
├─────────────────────────────────────────────────────────┤
│                    Data Access Layer                    │
├─────────────────────────────────────────────────────────┤
│ - UserRepository (JPA)                                 │
│ - UserMapper (MapStruct)                               │
├─────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                   │
├─────────────────────────────────────────────────────────┤
│ - MySQL Database                                        │
│ - AWS S3 Storage                                        │
│ - Kafka Message Broker                                  │
│ - gRPC Communication                                    │
└─────────────────────────────────────────────────────────┘
```

### 2.2 데이터 모델

#### 2.2.1 User Entity
```java
@Entity
public class User {
    @Id
    private String userId;           // 사용자 고유 ID

    @Column(unique = true)
    private String email;            // 이메일 (중복 불가)

    private String name;             // 사용자 이름
    private String password;         // 암호화된 비밀번호

    @Enumerated(EnumType.STRING)
    private UserRole role;           // 사용자 권한 (USER, ADMIN)

    private Long remainToken;        // 잔여 토큰 (기본값: 10,000)
    private String profile;          // 프로필 이미지 URL

    @CreatedDate
    private LocalDateTime createdAt; // 생성 시간
}
```

#### 2.2.2 UserRole Enum
```java
public enum UserRole {
    USER,    // 일반 사용자
    ADMIN    // 관리자
}
```

### 2.3 데이터베이스 설계

#### 2.3.1 users 테이블
```sql
CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    remain_token BIGINT NOT NULL DEFAULT 10000,
    profile VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
```

#### 2.3.2 인덱스 전략
- **Primary Index**: user_id (기본키)
- **Unique Index**: email (중복 방지 및 빠른 검색)
- **Secondary Index**: role (권한별 필터링)
