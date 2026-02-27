# [Stage 1] 빌드 단계
FROM gradle:jdk17 AS builder
WORKDIR /app

# 의존성 파일만 먼저 복사 → 소스 변경 시에도 이 레이어는 캐시 재사용
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# 소스 복사 및 빌드
COPY src ./src
RUN gradle build -x test --no-daemon
RUN rm -f build/libs/*-plain.jar

# [Stage 2] 실행 단계
FROM amazoncorretto:17
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
