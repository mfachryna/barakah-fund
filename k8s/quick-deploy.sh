#!/bin/bash

echo "Barakah Fund - Quick Deploy to Docker Desktop Kubernetes"
echo "======================================================="

if ! kubectl cluster-info &> /dev/null; then
    echo ""
    echo "Docker Desktop Kubernetes is not running"
    echo ""
    echo "Please enable Kubernetes in Docker Desktop:"
    echo "1. Open Docker Desktop"
    echo "2. Go to Settings (gear icon)"
    echo "3. Click on 'Kubernetes' tab"
    echo "4. Check 'Enable Kubernetes'"
    echo "5. Click 'Apply & Restart'"
    echo ""
    echo "Then run this script again."
    exit 1
fi

echo "Docker Desktop Kubernetes is running"

echo ""
echo "🔨 Building Docker images..."
././k8s/scripts/build-images.sh

if [ $? -ne 0 ]; then
    echo "Image build failed"
    exit 1
fi

echo ""
echo "Deploying to Kubernetes..."
./k8s/scripts/deploy.sh

if [ $? -ne 0 ]; then
    echo "Deployment failed"
    exit 1
fi

echo ""
echo "Deployment completed successfully!"
echo ""
echo "Your services are now running:"
echo "=================================="
echo "Main Gateway: http://localhost:8070"
echo "Keycloak Admin: http://localhost:8080"
echo "Grafana Dashboard: http://localhost:3000"
echo "Prometheus Metrics: http://localhost:9090"
echo ""
echo "Default Credentials:"
echo "Keycloak Admin: admin/admin123"
echo "Grafana: admin/admin123"
echo ""
echo "Check status: ./k8s/status.sh"
echo "Cleanup: ./k8s/scripts/cleanup.sh"