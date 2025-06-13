FROM maven:3.9.6-eclipse-temurin-17 AS builder

LABEL maintainer="Barakah Fund Team"
LABEL service="user-service"
LABEL version="1.0.0"

WORKDIR /workspace

COPY proto/ proto/
COPY shared/ shared/
COPY user-service/ user-service/
RUN rm -rf /workspace/user-service/src/main/resources

WORKDIR /workspace/proto
RUN --mount=type=cache,target=/root/.m2 mvn clean install -DskipTests

WORKDIR /workspace/shared
RUN --mount=type=cache,target=/root/.m2 mvn clean install -DskipTests

WORKDIR /workspace/user-service
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /workspace/user-service/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R appuser:appuser /app

USER appuser

EXPOSE 8081 9091

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/user-service/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]