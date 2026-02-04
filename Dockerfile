FROM gradle:7.6.1-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# [Stage 2] 실행 단계 (여기를 수정했습니다!)
# openjdk 대신 AWS 표준인 Amazon Corretto 사용 -> 에러 해결
FROM amazoncorretto:17
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]