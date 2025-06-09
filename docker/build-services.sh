#!/bin/bash
set -e

echo "Building Barakah Fund Microservices..."


cd "$(dirname "$0")/.."


echo "Building proto module..."
cd proto
mvn clean install -DskipTests
cd ..

echo "Building shared module..."
cd shared
mvn clean install -DskipTests
cd ..

echo "Proto and shared modules built successfully!"


echo "Building config-server..."
cd config-server
mvn clean package -DskipTests
cd ..

echo "Building eureka-server..."
cd eureka-server
mvn clean package -DskipTests
cd ..


echo "Building user-service..."
cd user-service
mvn clean package -DskipTests
cd ..

echo "Building account-service..."
cd account-service
mvn clean package -DskipTests
cd ..

echo "Building transaction-service..."
cd transaction-service
mvn clean package -DskipTests
cd ..

echo "Building gateway-service..."
cd gateway-service
mvn clean package -DskipTests
cd ..

echo "All services built successfully!"


echo "Building Docker images..."
cd docker

echo "Building infrastructure services..."
docker-compose -f compose-dev-vault.yml build config-server eureka-server

echo "Building business services..."
docker-compose -f compose-dev-vault.yml build user-service account-service transaction-service gateway-service

echo "All Docker images built successfully!"
