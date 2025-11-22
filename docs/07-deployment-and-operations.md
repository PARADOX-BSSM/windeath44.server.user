# User Server PRD - 배포 및 운영

## 1. 컨테이너화

### 1.1 Docker 설정

#### 1.1.1 Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

# 메타데이터
LABEL maintainer="windeath44@example.com"
LABEL version="1.0.0"
LABEL description="User Management Service for 최애의 사인"

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 의존성 설치
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# JAR 파일 복사
COPY target/user-service-*.jar app.jar

# 애플리케이션 사용자 생성 (보안 강화)
RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring

# 포트 노출
EXPOSE 8080

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### 1.1.2 Multi-stage build (최적화)
```dockerfile
# Build stage
FROM gradle:7.6-jdk17 AS builder

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY src src

RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 1.2 Docker Compose (로컬 개발)
```yaml
# docker-compose.yml
version: '3.8'

services:
  user-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=userdb
      - DB_USERNAME=root
      - DB_PASSWORD=password
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - S3_BUCKET=dev-user-profiles
    depends_on:
      - mysql
      - kafka
      - zookeeper
    networks:
      - user-network

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: userdb
    volumes:
      - mysql_data:/var/lib/mysql
      - ./scripts/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    networks:
      - user-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - user-network

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - user-network

volumes:
  mysql_data:

networks:
  user-network:
    driver: bridge
```

## 2. Kubernetes 배포

### 2.1 Deployment 설정
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: user-system
  labels:
    app: user-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
        version: v1
    spec:
      containers:
      - name: user-service
        image: user-service:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: user-service-secrets
              key: db-host
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: user-service-secrets
              key: db-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: user-service-secrets
              key: db-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
      volumes:
      - name: config-volume
        configMap:
          name: user-service-config
```

### 2.2 Service 및 Ingress
```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: user-system
  labels:
    app: user-service
spec:
  selector:
    app: user-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP

---
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: user-service-ingress
  namespace: user-system
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.choi-ae.com
    secretName: user-service-tls
  rules:
  - host: api.choi-ae.com
    http:
      paths:
      - path: /users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 80
```

### 2.3 ConfigMap 및 Secrets
```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
  namespace: user-system
data:
  application.yml: |
    spring:
      application:
        name: user-service
      jpa:
        hibernate:
          ddl-auto: validate
        show-sql: false
      kafka:
        producer:
          retries: 3
          batch-size: 16384

    management:
      endpoints:
        web:
          exposure:
            include: health,metrics,prometheus
      endpoint:
        health:
          show-details: always

---
# k8s/secrets.yaml (실제로는 암호화된 상태로 관리)
apiVersion: v1
kind: Secret
metadata:
  name: user-service-secrets
  namespace: user-system
type: Opaque
data:
  db-host: <base64-encoded-value>
  db-username: <base64-encoded-value>
  db-password: <base64-encoded-value>
  jwt-secret: <base64-encoded-value>
  kafka-username: <base64-encoded-value>
  kafka-password: <base64-encoded-value>
```

### 2.4 Auto Scaling 설정
```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: user-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
```

## 3. CI/CD 파이프라인

### 3.1 GitHub Actions 워크플로우
```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: user-service

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: ~/.gradle/caches
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle') }}
        restore-keys: ${{ runner.os }}-gradle

    - name: Run tests
      run: ./gradlew test

    - name: Generate test report
      run: ./gradlew jacocoTestReport

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3

  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Run Snyk to check for vulnerabilities
      uses: snyk/actions/gradle@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}

    - name: Run OWASP Dependency Check
      run: ./gradlew dependencyCheckAnalyze

  build-and-push:
    needs: [test, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Build application
      run: ./gradlew bootJar

    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=sha,prefix={{branch}}-
          type=raw,value=latest

    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}

  deploy-staging:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging

    steps:
    - name: Deploy to staging
      run: |
        echo "Deploying to staging environment..."
        # kubectl 명령어로 스테이징 환경에 배포

  deploy-production:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production

    steps:
    - name: Deploy to production
      run: |
        echo "Deploying to production environment..."
        # Blue-Green 배포 스크립트 실행
```

### 3.2 Blue-Green 배포 스크립트
```bash
#!/bin/bash
# scripts/blue-green-deploy.sh

set -e

NAMESPACE="user-system"
APP_NAME="user-service"
NEW_IMAGE="$1"
TIMEOUT=300

echo "🚀 Starting Blue-Green deployment for $APP_NAME"
echo "📦 New image: $NEW_IMAGE"

# 현재 활성 환경 확인
CURRENT_ENV=$(kubectl get service $APP_NAME -n $NAMESPACE -o jsonpath='{.spec.selector.env}')
echo "📍 Current environment: $CURRENT_ENV"

# 새 환경 결정
if [ "$CURRENT_ENV" = "blue" ]; then
    NEW_ENV="green"
else
    NEW_ENV="blue"
fi

echo "🎯 Target environment: $NEW_ENV"

# 새 환경에 배포
echo "📤 Deploying to $NEW_ENV environment..."
kubectl set image deployment/$APP_NAME-$NEW_ENV $APP_NAME=$NEW_IMAGE -n $NAMESPACE

# 배포 완료 대기
echo "⏳ Waiting for rollout to complete..."
kubectl rollout status deployment/$APP_NAME-$NEW_ENV -n $NAMESPACE --timeout=${TIMEOUT}s

# 헬스체크
echo "🔍 Performing health check..."
kubectl wait --for=condition=ready pod -l app=$APP_NAME,env=$NEW_ENV -n $NAMESPACE --timeout=120s

# 스모크 테스트
echo "🧪 Running smoke tests..."
NEW_POD=$(kubectl get pods -n $NAMESPACE -l app=$APP_NAME,env=$NEW_ENV -o jsonpath='{.items[0].metadata.name}')
kubectl exec $NEW_POD -n $NAMESPACE -- curl -f http://localhost:8080/actuator/health

# 트래픽 전환
echo "🔄 Switching traffic to $NEW_ENV..."
kubectl patch service $APP_NAME -n $NAMESPACE -p '{"spec":{"selector":{"env":"'$NEW_ENV'"}}}'

# 이전 환경 스케일 다운 (30초 후)
echo "⏳ Waiting 30 seconds before scaling down old environment..."
sleep 30

echo "📉 Scaling down $CURRENT_ENV environment..."
kubectl scale deployment $APP_NAME-$CURRENT_ENV --replicas=0 -n $NAMESPACE

echo "✅ Blue-Green deployment completed successfully!"
```

## 4. 환경별 구성

### 4.1 개발 환경 (Development)
```yaml
# config/application-dev.yml
spring:
  profiles:
    active: dev

  datasource:
    url: jdbc:mysql://localhost:3306/userdb_dev
    username: dev_user
    password: dev_pass

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  kafka:
    bootstrap-servers: localhost:9092

logging:
  level:
    io.windeath44: DEBUG
    org.springframework: INFO

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

### 4.2 스테이징 환경 (Staging)
```yaml
# config/application-staging.yml
spring:
  profiles:
    active: staging

  datasource:
    url: jdbc:mysql://staging-db:3306/userdb_staging
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

logging:
  level:
    io.windeath44: INFO
    org.springframework: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
```

### 4.3 프로덕션 환경 (Production)
```yaml
# config/application-prod.yml
spring:
  profiles:
    active: prod

  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: PLAIN
      jaas:
        config: org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";

logging:
  level:
    io.windeath44: INFO
    org.springframework: WARN
    org.hibernate: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  endpoint:
    health:
      show-details: when-authorized
```

## 5. 운영 도구

### 5.1 모니터링 설정
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-service:8080']
    scrape_interval: 5s

  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - user-system
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
```

### 5.2 로깅 집중화
```yaml
# logging/fluentd-config.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-config
data:
  fluent.conf: |
    <source>
      @type tail
      @id in_tail_container_logs
      path /var/log/containers/user-service*.log
      pos_file /var/log/fluentd-containers.log.pos
      tag user-service.*
      read_from_head true
      <parse>
        @type json
        time_format %Y-%m-%dT%H:%M:%S.%NZ
      </parse>
    </source>

    <filter user-service.**>
      @type parser
      key_name log
      reserve_data true
      <parse>
        @type json
      </parse>
    </filter>

    <match user-service.**>
      @type elasticsearch
      host elasticsearch
      port 9200
      index_name user-service-logs
      type_name _doc
      <buffer>
        flush_interval 5s
      </buffer>
    </match>
```

### 5.3 백업 및 복구
```bash
#!/bin/bash
# scripts/backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/user-service"
DB_HOST="${DB_HOST}"
DB_NAME="${DB_NAME}"
DB_USER="${DB_USER}"
DB_PASS="${DB_PASS}"

echo "🗄️  Starting backup process..."

# 데이터베이스 백업
echo "📊 Backing up database..."
mysqldump -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME \
  --single-transaction --routines --triggers \
  | gzip > $BACKUP_DIR/db_backup_$DATE.sql.gz

# S3에 백업 파일 업로드
echo "☁️  Uploading to S3..."
aws s3 cp $BACKUP_DIR/db_backup_$DATE.sql.gz \
  s3://user-service-backups/database/

# 로컬 백업 파일 정리 (7일 이상된 파일 삭제)
echo "🧹 Cleaning up old backup files..."
find $BACKUP_DIR -name "db_backup_*.sql.gz" -mtime +7 -delete

echo "✅ Backup completed: db_backup_$DATE.sql.gz"
```