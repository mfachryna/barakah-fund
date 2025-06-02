#!/bin/bash

set -e

echo "Building Barakah Fund Microservices..."

# Navigate to project root
cd "$(dirname "$0")/.."

echo "Building proto module..."
./mvnw clean install -pl proto -am -DskipTests

echo "Building shared module..."
./mvnw clean install -pl shared -am -DskipTests

echo "Building config-server..."
./mvnw clean package -pl config-server -am -DskipTests

echo "Building eureka-server..."
./mvnw clean package -pl eureka-server -am -DskipTests

echo "Building user-service..."
./mvnw clean package -pl user-service -am -DskipTests

echo "Building account-service..."
./mvnw clean package -pl account-service -am -DskipTests

echo "Building transaction-service..."
./mvnw clean package -pl transaction-service -am -DskipTests

echo "Building gateway-service..."
./mvnw clean package -pl gateway-service -am -DskipTests

echo "All services built successfully!"

echo "Building Docker images..."
cd docker

echo "Building infrastructure services..."
docker-compose -f compose-dev-novault.yml build config-server eureka-server

echo "Building business services..."
docker-compose -f compose-dev-novault.yml build user-service account-service transaction-service gateway-service

echo "All Docker images built successfully!"
