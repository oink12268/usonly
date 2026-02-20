# [Stage 1] 빌드 단계
FROM gradle:jdk17 AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# ★ [추가된 부분] 필요 없는 '-plain.jar' 파일을 삭제합니다.
# 이러면 폴더에 실행 파일 딱 1개만 남아서 에러가 안 납니다.
RUN rm -f build/libs/*-plain.jar

# [Stage 2] 실행 단계
FROM amazoncorretto:17
WORKDIR /app
# 이제 *.jar는 파일 1개만 가리키므로 에러 없이 복사됩니다!
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]