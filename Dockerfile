# 1. 자바 17 환경을 가져옵니다.
FROM openjdk:17-jdk-slim

# 2. 빌드된 jar 파일의 위치를 변수로 지정합니다. (Gradle 기준)
# Maven을 쓰신다면 target/*.jar 로 바꿔주세요.
ARG JAR_FILE=build/libs/*.jar

# 3. jar 파일을 app.jar라는 이름으로 복사합니다.
COPY ${JAR_FILE} app.jar

# 4. 서버를 실행합니다.
ENTRYPOINT ["java", "-jar", "/app.jar"]