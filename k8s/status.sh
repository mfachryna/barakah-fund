
#!/bin/bash

echo "Barakah Fund Kubernetes Status (Docker Desktop)"
echo "=============================================="

if ! kubectl cluster-info &> /dev/null; then
    echo "Docker Desktop Kubernetes is not running"
    echo "Please enable Kubernetes in Docker Desktop settings"
    exit 1
fi

echo ""
echo "Docker Desktop Kubernetes is running"
echo ""

echo "Pods in barakah-fund namespace:"
kubectl get pods -n barakah-fund -o wide

echo ""
echo "Pods in monitoring namespace:"
kubectl get pods -n monitoring -o wide

echo ""
echo "Services:"
echo "Barakah Fund Services:"
kubectl get services -n barakah-fund
echo ""
echo "Monitoring Services:"
kubectl get services -n monitoring

echo ""
echo "Persistent Volumes:"
kubectl get pv,pvc -n barakah-fund

echo ""
echo "Access URLs:"
echo "==============================================="
echo "Gateway Service: http://localhost:8070"
echo "Keycloak: http://localhost:8080"
echo "Grafana: http://localhost:3000"
echo "Prometheus: http://localhost:9090"
echo ""
echo "Port Forwarding (if LoadBalancer doesn't work):"
echo "kubectl port-forward service/gateway-service 8070:8070 -n barakah-fund"
echo "kubectl port-forward service/grafana-service 3000:3000 -n monitoring"
echo "kubectl port-forward service/prometheus-service 9090:9090 -n monitoring"