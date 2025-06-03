#!/bin/bash

echo "Building Docker images for Barakah Fund services (Docker Desktop)..."

PROJECT_ROOT="/Users/mfachryna/Documents/kodingan/BSI/ojt_spesifik/barakah-fund"
cd $PROJECT_ROOT

TAG=${1:-latest}

build_service() {
    local service_name=$1
    local dockerfile_path=$2
    
    echo "Building $service_name..."
    docker build -f $dockerfile_path -t barakah/$service_name:$TAG . || {
        echo "Failed to build $service_name"
        exit 1
    }
    echo "$service_name built successfully"
}

build_service "eureka-server" "k8s/dockerfiles/eureka.Dockerfile"
build_service "user-service" "k8s/dockerfiles/user.Dockerfile"
build_service "account-service" "k8s/dockerfiles/account.Dockerfile"
build_service "transaction-service" "k8s/dockerfiles/transaction.Dockerfile"
build_service "gateway-service" "k8s/dockerfiles/gateway.Dockerfile"

echo ""
echo "All images built successfully!"

if kubectl cluster-info &> /dev/null; then
    echo "Docker Desktop Kubernetes is running"
    echo "Images are ready for deployment (using same Docker daemon)"
else
    echo "Docker Desktop Kubernetes is not running"
    echo "Please enable Kubernetes in Docker Desktop settings"
    exit 1
fi

echo "Build process completed!"