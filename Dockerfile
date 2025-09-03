# 1단계: 빌드 스테이지
FROM amazoncorretto:17 AS builder

WORKDIR /app

# 환경변수 설정
ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=3.0-M12

COPY . .

RUN chmod +x ./gradlew && ./gradlew bootJar && \
    cp build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar app.jar

# 2단계: 실행 스테이지
FROM amazoncorretto:17-alpine AS runtime

WORKDIR /app

ENV JVM_OPTS=""
EXPOSE 8080

RUN apk add --no-cache curl

COPY --from=builder /app/app.jar app.jar

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
