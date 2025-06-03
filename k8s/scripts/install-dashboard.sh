#!/bin/bash

echo "Installing Kubernetes Dashboard"
echo "=================================="

echo "Installing Kubernetes Dashboard..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

echo "Waiting for dashboard pods to be ready..."
kubectl wait --for=condition=ready pod -l k8s-app=kubernetes-dashboard -n kubernetes-dashboard --timeout=300s

echo "Kubernetes Dashboard installed successfully!"
echo ""