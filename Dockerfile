# 1단계: 빌드 환경 (Gradle이 깔린 이미지 사용)
FROM gradle:7.6.1-jdk17 AS builder
WORKDIR /app
COPY . .
# 도커 안에서 빌드 실행 (테스트 제외)
RUN ./gradlew clean build -x test

# 2단계: 실행 환경 (가벼운 Java 이미지만 사용)
FROM openjdk:17-jdk-slim
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]