#!/bin/bash

set -e

echo "Building Docker images for Barakah Fund Microservices..."

# Build infrastructure services first
echo "Building infrastructure services..."
docker build -t barakah/config-server:latest ../config-server
docker build -t barakah/eureka-server:latest ../eureka-server

# Build business services
echo "Building business services..."
docker build -t barakah/user-service:latest ../user-service
docker build -t barakah/account-service:latest ../account-service
docker build -t barakah/transaction-service:latest ../transaction-service
docker build -t barakah/gateway-service:latest ../gateway-service

echo "All Docker images built successfully!"

# Optional: List built images
echo "Built images:"
docker images | grep barakah
