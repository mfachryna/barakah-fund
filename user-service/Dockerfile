FROM openjdk:17-jdk-slim

LABEL maintainer="Barakah Fund Team"
LABEL service="user-service"
LABEL version="1.0.0"

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

RUN mkdir -p /app/logs && chown -R appuser:appuser /app

COPY target/user-service-1.0.0.jar app.jar

RUN chown appuser:appuser app.jar

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

USER appuser

EXPOSE 8081 9091

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/user-service/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]