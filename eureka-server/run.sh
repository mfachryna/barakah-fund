#!/bin/bash

echo "Starting Barakah Fund Eureka Server..."

export EUREKA_USERNAME=${EUREKA_USERNAME:-eureka-user}
export EUREKA_PASSWORD=${EUREKA_PASSWORD:-eureka-pass}
export CONFIG_SERVER_URI=${CONFIG_SERVER_URI:-http://localhost:8888}
export CONFIG_SERVER_USERNAME=${CONFIG_SERVER_USERNAME:-config-user}
export CONFIG_SERVER_PASSWORD=${CONFIG_SERVER_PASSWORD:-config-pass}

if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven to continue."
    exit 1
fi

echo "Building application..."
mvn clean package -DskipTests

echo "Starting Config Server on port 8888..."
mvn spring-boot:run

# echo "Config Server should be available at http://localhost:8888"
# echo "Health check: http://localhost:8888/actuator/health"
# echo "Use credentials: $CONFIG_SERVER_USERNAME / $CONFIG_SERVER_PASSWORD"