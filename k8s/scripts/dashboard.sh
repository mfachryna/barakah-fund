#!/bin/bash

echo "🎛️  Kubernetes Dashboard Access"
echo "==============================="

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

if ! kubectl get namespace kubernetes-dashboard &> /dev/null; then
    echo "Kubernetes Dashboard is not installed"
    echo "Run: ./k8s/scripts/setup-dashboard.sh"
    exit 1
fi

if ! kubectl get pods -n kubernetes-dashboard -l k8s-app=kubernetes-dashboard --field-selector=status.phase=Running &> /dev/null; then
    echo "Kubernetes Dashboard is not running"
    echo "Please check the installation"
    exit 1
fi

echo "Kubernetes Dashboard is running"
echo ""

get_admin_token() {
    kubectl -n kubernetes-dashboard create token admin-user 2>/dev/null
}

get_readonly_token() {
    kubectl -n kubernetes-dashboard create token readonly-user 2>/dev/null
}

start_proxy() {
    echo "Starting kubectl proxy..."
    echo "Dashboard will be available at:"
    echo -e "${BLUE}http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/${NC}"
    echo ""
    echo "Keep this terminal open while using the dashboard"
    echo "Press Ctrl+C to stop the proxy"
    echo ""
    

    kubectl proxy
}

while true; do
    echo -e "${YELLOW}Kubernetes Dashboard Menu${NC}"
    echo "========================"
    echo ""
    echo "1. Start Dashboard (kubectl proxy)"
    echo "2. Get Admin Token (full access)"
    echo "3. Get Readonly Token (view only)"
    echo "4. Show Dashboard Status"
    echo "5. Open Dashboard URL"
    echo "6. Show Access Instructions"
    echo "0. Exit"
    echo ""
    read -p "Select option: " choice
    
    case $choice in
        1)
            clear
            start_proxy
            ;;
        2)
            echo ""
            echo -e "${GREEN}Admin Token (copy this):${NC}"
            echo "=============================="
            get_admin_token
            echo ""
            read -p "Press Enter to continue..."
            ;;
        3)
            echo ""
            echo -e "${GREEN}Readonly Token (copy this):${NC}"
            echo "================================"
            get_readonly_token
            echo ""
            read -p "Press Enter to continue..."
            ;;
        4)
            echo ""
            echo "Dashboard Status:"
            echo "==================="
            kubectl get pods -n kubernetes-dashboard
            echo ""
            kubectl get services -n kubernetes-dashboard
            echo ""
            read -p "Press Enter to continue..."
            ;;
        5)
            echo ""
            echo "Opening Dashboard URL..."
            open "http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/" 2>/dev/null || echo "Please open: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
            echo ""
            read -p "Press Enter to continue..."
            ;;
        6)
            clear
            echo -e "${BLUE}Dashboard Access Instructions${NC}"
            echo "================================"
            echo ""
            echo "1. Start the dashboard proxy:"
            echo "   kubectl proxy"
            echo ""
            echo "2. Open your browser and go to:"
            echo "   http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
            echo ""
            echo "3. Select 'Token' authentication method"
            echo ""
            echo "4. Get a token using:"
            echo "   Admin token: kubectl -n kubernetes-dashboard create token admin-user"
            echo "   Readonly token: kubectl -n kubernetes-dashboard create token readonly-user"
            echo ""
            echo "5. Paste the token and click 'Sign In'"
            echo ""
            echo "What you can do in the dashboard:"
            echo "   • View all pods, services, deployments"
            echo "   • Check logs and events"
            echo "   • Monitor resource usage"
            echo "   • Scale deployments"
            echo "   • View cluster metrics"
            echo "   • Manage configurations"
            echo ""
            read -p "Press Enter to continue..."
            ;;
        0)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo "Invalid option. Please try again."
            ;;
    esac
    clear
done