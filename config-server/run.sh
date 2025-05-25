#!/bin/bash

echo "Starting Barakah Fund Config Server..."

export CONFIG_SERVER_USERNAME=${CONFIG_SERVER_USERNAME:-config-user}
export CONFIG_SERVER_PASSWORD=${CONFIG_SERVER_PASSWORD:-config-pass}
export CONFIG_GIT_URI=${CONFIG_GIT_URI:-https://github.com/mfachryna/barakah-fund-config.git}

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