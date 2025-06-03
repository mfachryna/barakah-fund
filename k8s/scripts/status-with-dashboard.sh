
#!/bin/bash
echo "Barakah Fund Kubernetes Status & Dashboard"
echo "=============================================="

# Check if dashboard is available
DASHBOARD_STATUS="Not Installed"
if kubectl get namespace kubernetes-dashboard &> /dev/null; then
    if kubectl get pods -n kubernetes-dashboard -l k8s-app=kubernetes-dashboard --field-selector=status.phase=Running &> /dev/null; then
        DASHBOARD_STATUS="Running"
    else
        DASHBOARD_STATUS="Installed but not running"
    fi
fi

echo ""
echo "System Overview"
echo "=================="
echo "K8s Dashboard: $DASHBOARD_STATUS"
echo "Grafana: http://localhost:3000"
echo "Prometheus: http://localhost:9090"
echo "Gateway: http://localhost:8070"
echo ""

echo "Pod Status"
echo "============="
kubectl get pods -n barakah-fund -o custom-columns=NAME:.metadata.name,STATUS:.status.phase,READY:.status.containerStatuses[0].ready,RESTARTS:.status.containerStatuses[0].restartCount,AGE:.metadata.creationTimestamp
echo ""

echo "Services"
echo "==========="
kubectl get services -n barakah-fund
echo ""

echo "Dashboard Access"
echo "=================="
if [[ "$DASHBOARD_STATUS" == "Running" ]]; then
    echo "Dashboard is ready! Run:"
    echo "./k8s/scripts/dashboard.sh"
    echo ""
    echo "Or quick access:"
    echo "./k8s/scripts/quick-dashboard.sh"
else
    echo "Install dashboard with:"
    echo "./k8s/scripts/setup-dashboard.sh"
fi
echo ""

echo "Quick Commands"
echo "================="
echo "View logs: kubectl logs -f deployment/SERVICE-NAME -n barakah-fund"
echo "Scale service: kubectl scale deployment SERVICE-NAME --replicas=N -n barakah-fund"
echo "Restart service: kubectl rollout restart deployment/SERVICE-NAME -n barakah-fund"
echo "Port forward: kubectl port-forward service/SERVICE-NAME LOCAL:REMOTE -n NAMESPACE"