# 1단계: Build
FROM gradle:8.4-jdk17 AS build
WORKDIR /app

COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY . .
RUN ./gradlew bootJar --no-daemon

# 2단계: Runtime
FROM openjdk:17-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 4444
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
