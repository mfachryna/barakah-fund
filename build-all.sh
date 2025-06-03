#!/bin/bash

echo "Barakah Fund - Build Images for Both Docker Compose and Kubernetes"
echo "=================================================================="

PROJECT_ROOT="/Users/mfachryna/Documents/kodingan/BSI/ojt_spesifik/barakah-fund"
cd $PROJECT_ROOT

TAG=${1:-latest}
TARGET=${2:-all} 

build_for_compose() {
    echo ""
    echo "Building images for Docker Compose..."
    echo "========================================"
    
    docker-compose -f docker/compose-dev-novault.yml build --parallel
    
    if [ $? -eq 0 ]; then
        echo "Docker Compose images built successfully"
    else
        echo "Docker Compose build failed"
        return 1
    fi
}

build_for_k8s() {
    echo ""
    echo "Building images for Kubernetes..."
    echo "===================================="
    
    ./k8s/scripts/build-images.sh $TAG
    
    if [ $? -eq 0 ]; then
        echo "Kubernetes images built successfully"
    else
        echo "Kubernetes build failed"
        return 1
    fi
}

case $TARGET in
    compose)
        build_for_compose
        ;;
    k8s)
        build_for_k8s
        ;;
    all)
        build_for_compose && build_for_k8s
        ;;
    *)
        echo "Usage: $0 [tag] [target]"
        echo "  tag: Image tag (default: latest)"
        echo "  target: compose, k8s, or all (default: all)"
        exit 1
        ;;
esac

echo ""
echo "🎉 Build process completed!"
echo ""
echo "Available commands:"
echo "• Docker Compose: docker-compose -f docker/compose-dev-novault.yml up -d"
echo "• Kubernetes: ./k8s/quick-deploy.sh"