# 1단계: Gradle로 Spring Boot 애플리케이션 빌드
FROM --platform=linux/arm64 gradle:8.4-jdk17 AS build
WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle dependencies --no-daemon || true

# 애플리케이션 전체 복사 및 빌드
COPY . .
RUN gradle bootJar --no-daemon

# 2단계: 런타임 스테이지 (경량 JDK 환경에서 애플리케이션 실행)
FROM --platform=linux/arm64 openjdk:17-jdk

WORKDIR /app

# .env 파일을 컨테이너로 복사
COPY .env .env

# 빌드한 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너에서 열 포트 설정 (4445 포트)
EXPOSE 4445

CMD ["/bin/bash", "-c", "set -a; source /app/.env; set +a; java -jar /app/app.jar"]

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]