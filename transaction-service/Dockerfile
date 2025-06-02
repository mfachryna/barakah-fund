FROM maven:3.9.6-eclipse-temurin-17 AS builder

LABEL maintainer="Barakah Fund Team"
LABEL service="transaction-service"
LABEL version="1.0.0"

# Set working directory
WORKDIR /workspace

# First, build and install proto module
COPY proto/pom.xml proto/pom.xml
COPY proto/src proto/src
RUN --mount=type=cache,target=/root/.m2 cd proto && mvn clean install -DskipTests

# Then, build and install shared module
COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
RUN --mount=type=cache,target=/root/.m2 cd shared && mvn clean install -DskipTests

# Finally, build the transaction service
COPY transaction-service/pom.xml .
COPY transaction-service/src ./src

# Download dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# Build the service
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

# Runtime stage
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8083 9093

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/transaction-service/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
