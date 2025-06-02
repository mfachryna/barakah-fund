FROM maven:3.9.6-eclipse-temurin-17 AS builder

LABEL maintainer="Barakah Fund Team"
LABEL service="gateway-service"
LABEL version="1.0.0"

WORKDIR /workspace

COPY proto/pom.xml proto/pom.xml
COPY proto/src proto/src
RUN --mount=type=cache,target=/root/.m2 cd proto && mvn clean install -DskipTests

COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
RUN --mount=type=cache,target=/root/.m2 cd shared && mvn clean install -DskipTests

COPY gateway-service/pom.xml .
COPY gateway-service/src ./src

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /workspace/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R appuser:appuser /app

USER appuser

EXPOSE 8070 9090

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8070/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
