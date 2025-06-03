# k8s/scripts/quick-dashboard.sh
#!/bin/bash

echo "Quick Kubernetes Dashboard Access"
echo "===================================="

# Check if dashboard exists
if ! kubectl get namespace kubernetes-dashboard &> /dev/null; then
    echo "Installing Kubernetes Dashboard..."
    ./k8s/scripts/setup-dashboard.sh
fi

# Get admin token
echo "Getting access token..."
TOKEN=$(kubectl -n kubernetes-dashboard create token admin-user)

echo ""
echo "Dashboard is ready!"
echo ""
echo "Dashboard URL: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo ""
echo "Your access token:"
echo "===================="
echo "$TOKEN"
echo "===================="
echo ""
echo "Quick Steps:"
echo "1. Copy the token above"
echo "2. Wait for the browser to open"
echo "3. Select 'Token' and paste the token"
echo "4. Click 'Sign In'"
echo ""

# Copy token to clipboard (Mac)
echo "$TOKEN" | pbcopy 2>/dev/null && echo "Token copied to clipboard!"

echo "Starting dashboard..."
sleep 2

# Open browser
open "http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/" 2>/dev/null &

# Start proxy
kubectl proxy