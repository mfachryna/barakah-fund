#!/bin/bash

echo "Setting up port forwarding for Docker Desktop Kubernetes"
echo "======================================================="

start_port_forward() {
    local service=$1
    local port=$2
    local namespace=$3
    local display_name=$4
    
    echo "Starting port-forward for $display_name..."
    kubectl port-forward service/$service $port:$port -n $namespace > /dev/null 2>&1 &
    echo "   Accessible at: http://localhost:$port"
}

echo "Cleaning up existing port-forwards..."
pkill -f "kubectl port-forward"
sleep 2

start_port_forward "gateway-service" "8070" "barakah-fund" "Gateway Service"
start_port_forward "keycloak-service" "8080" "barakah-fund" "Keycloak"
start_port_forward "grafana-service" "3000" "monitoring" "Grafana"
start_port_forward "prometheus-service" "9090" "monitoring" "Prometheus"

echo ""
echo "Port forwarding setup complete!"
echo ""
echo "Access your services:"
echo "Gateway: http://localhost:8070"
echo "Keycloak: http://localhost:8080"
echo "Grafana: http://localhost:3000"
echo "Prometheus: http://localhost:9090"
echo ""
echo "⚠️  Keep this terminal open to maintain port forwarding"
echo "⚠️  Press Ctrl+C to stop all port forwards"
echo ""

trap 'echo ""; echo "Stopping port forwards..."; pkill -f "kubectl port-forward"; exit 0' INT
while true; do sleep 1; done