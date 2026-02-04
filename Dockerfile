# [Stage 1] 빌드 단계
# alpine(경량화) 대신 호환성 좋은 정식 버전(jdk17) 사용 -> ARM 에러 해결!
FROM gradle:jdk17 AS builder
WORKDIR /app
COPY . .
# 실행 권한 부여 후 빌드 (혹시 모를 권한 문제 예방)
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# [Stage 2] 실행 단계
# AWS 표준인 Amazon Corretto 사용 (ARM 완벽 지원)
FROM amazoncorretto:17
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]