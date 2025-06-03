#!/bin/bash

echo "🎛️  Setting up Kubernetes Dashboard UI"
echo "====================================="

PROJECT_ROOT="/Users/mfachryna/Documents/kodingan/BSI/ojt_spesifik/barakah-fund"
K8S_DIR="$PROJECT_ROOT/k8s"

mkdir -p $K8S_DIR/dashboard

if ! kubectl cluster-info &> /dev/null; then
    echo "Docker Desktop Kubernetes is not running"
    echo "Please enable Kubernetes in Docker Desktop settings"
    exit 1
fi

echo "Docker Desktop Kubernetes is running"

echo "Installing Kubernetes Dashboard..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

echo "Creating admin user..."
kubectl apply -f $K8S_DIR/dashboard/admin-user.yaml

echo "Creating readonly user..."
kubectl apply -f $K8S_DIR/dashboard/readonly-user.yaml

echo "Waiting for dashboard to be ready..."
kubectl wait --for=condition=ready pod -l k8s-app=kubernetes-dashboard -n kubernetes-dashboard --timeout=300s

echo ""
echo "Kubernetes Dashboard setup completed!"
echo ""

echo "Getting admin user token..."
ADMIN_TOKEN=$(kubectl -n kubernetes-dashboard create token admin-user)

echo "Getting readonly user token..."
READONLY_TOKEN=$(kubectl -n kubernetes-dashboard create token readonly-user)

echo ""
echo "Access Information:"
echo "====================="
echo ""
echo "Dashboard URL: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo ""
echo "Admin Token (full access):"
echo "$ADMIN_TOKEN"
echo ""
echo "Readonly Token (view only):"
echo "$READONLY_TOKEN"
echo ""
echo "To access the dashboard:"
echo "1. Run: kubectl proxy"
echo "2. Open: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo "3. Select 'Token' and paste one of the tokens above"
echo ""
echo "Tip: Use the readonly token for safe browsing"
echo "Keep the admin token secure!"