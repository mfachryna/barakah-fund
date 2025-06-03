#!/bin/bash

echo "Cleaning up Barakah Fund Kubernetes deployment..."

kubectl delete -f k8s/services/gateway-service.yaml --ignore-not-found=true
kubectl delete -f k8s/services/transaction-service.yaml --ignore-not-found=true
kubectl delete -f k8s/services/account-service.yaml --ignore-not-found=true
kubectl delete -f k8s/services/user-service.yaml --ignore-not-found=true
kubectl delete -f k8s/services/eureka-server.yaml --ignore-not-found=true
kubectl delete -f k8s/infrastructure/keycloak.yaml --ignore-not-found=true
kubectl delete -f k8s/infrastructure/kafka.yaml --ignore-not-found=true
kubectl delete -f k8s/infrastructure/redis.yaml --ignore-not-found=true
kubectl delete -f k8s/infrastructure/postgres.yaml --ignore-not-found=true

kubectl delete -f k8s/monitoring/grafana.yaml --ignore-not-found=true
kubectl delete -f k8s/monitoring/prometheus.yaml --ignore-not-found=true

kubectl delete -f k8s/infrastructure/persistent-volumes.yaml --ignore-not-found=true

kubectl delete -f k8s/infrastructure/configmaps.yaml --ignore-not-found=true
kubectl delete -f k8s/infrastructure/secrets.yaml --ignore-not-found=true

kubectl delete namespace barakah-fund --ignore-not-found=true
kubectl delete namespace monitoring --ignore-not-found=true

echo "Cleanup completed!"