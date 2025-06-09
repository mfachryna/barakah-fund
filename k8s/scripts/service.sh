#!/bin/bash
# filepath: /Users/mfachryna/Documents/kodingan/BSI/ojt_spesifik/barakah-fund/k8s/scripts/service.sh

set -e

SERVICE_NAME=$1
ACTION=$2
NAMESPACE="barakah-fund"

show_help() {
    echo "Barakah Fund Service Manager"
    echo "==========================="
    echo ""
    echo "Usage: $0 <service> <action>"
    echo ""
    echo "Services:"
    echo "  eureka       - Service Discovery"
    echo "  gateway      - API Gateway"
    echo "  user         - User Management"
    echo "  account      - Account Management"
    echo "  transaction  - Transaction Processing"
    echo "  notification - Notification Service"
    echo ""
    echo "Actions:"
    echo "  deploy       - Deploy/update service"
    echo "  restart      - Restart deployment"
    echo "  rebuild      - Rebuild image and restart"
    echo "  status       - Show service status"
    echo "  logs         - Show service logs"
    echo "  delete       - Delete service"
    echo "  scale        - Scale service (requires replicas count)"
    echo ""
    echo "Examples:"
    echo "  $0 gateway deploy      # Deploy gateway"
    echo "  $0 user restart        # Restart user service"
    echo "  $0 transaction rebuild # Rebuild and restart"
    echo "  $0 gateway logs        # Show gateway logs"
    echo "  $0 user scale 3        # Scale user service to 3 replicas"
}

if [[ "$1" == "-h" || "$1" == "--help" || -z "$1" ]]; then
    show_help
    exit 0
fi

# Service configurations
declare -A SERVICES
SERVICES["eureka"]="eureka-server:k8s/dockerfiles/eureka.Dockerfile:eureka-server:8761"
SERVICES["gateway"]="gateway-service:k8s/dockerfiles/gateway.Dockerfile:gateway-service:8070"
SERVICES["user"]="user-service:k8s/dockerfiles/user.Dockerfile:user-service:9091"
SERVICES["account"]="account-service:k8s/dockerfiles/account.Dockerfile:account-service:9092"
SERVICES["transaction"]="transaction-service:k8s/dockerfiles/transaction.Dockerfile:transaction-service:9093"
SERVICES["notification"]="notification-service:k8s/dockerfiles/notification.Dockerfile:notification-service:9094"

if [ -z "${SERVICES[$SERVICE_NAME]}" ]; then
    echo "❌ Unknown service: $SERVICE_NAME"
    echo "Available services: ${!SERVICES[@]}"
    exit 1
fi

IFS=':' read -r IMAGE_NAME DOCKERFILE DEPLOYMENT_NAME PORT <<< "${SERVICES[$SERVICE_NAME]}"

PROJECT_ROOT="/Users/mfachryna/Documents/kodingan/BSI/ojt_spesifik/barakah-fund"
cd "$PROJECT_ROOT"

case $ACTION in
    "deploy")
        echo "🚀 Deploying $SERVICE_NAME..."
        kubectl apply -f "k8s/services/${SERVICE_NAME}-service.yaml" -n $NAMESPACE
        kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE --timeout=300s
        ;;
        
    "restart")
        echo "🔄 Restarting $SERVICE_NAME..."
        kubectl rollout restart deployment/$DEPLOYMENT_NAME -n $NAMESPACE
        kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE --timeout=300s
        ;;
        
    "rebuild")
        echo "🔨 Rebuilding $SERVICE_NAME..."
        docker build -f "$DOCKERFILE" -t "barakah/$IMAGE_NAME:latest" .
        echo "🔄 Restarting deployment..."
        kubectl rollout restart deployment/$DEPLOYMENT_NAME -n $NAMESPACE
        kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE --timeout=300s
        ;;
        
    "status")
        echo "📊 Status for $SERVICE_NAME:"
        echo "Pods:"
        kubectl get pods -l app=$DEPLOYMENT_NAME -n $NAMESPACE
        echo ""
        echo "Service:"
        kubectl get svc $DEPLOYMENT_NAME -n $NAMESPACE
        echo ""
        echo "Deployment:"
        kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE
        ;;
        
    "logs")
        echo "📜 Logs for $SERVICE_NAME:"
        kubectl logs -l app=$DEPLOYMENT_NAME -n $NAMESPACE -f --tail=100
        ;;
        
    "delete")
        echo "🗑️  Deleting $SERVICE_NAME..."
        kubectl delete -f "k8s/services/${SERVICE_NAME}-service.yaml" -n $NAMESPACE
        ;;
        
    "scale")
        REPLICAS=$3
        if [ -z "$REPLICAS" ]; then
            echo "❌ Please specify number of replicas"
            echo "Usage: $0 $SERVICE_NAME scale <replicas>"
            exit 1
        fi
        echo "📈 Scaling $SERVICE_NAME to $REPLICAS replicas..."
        kubectl scale deployment/$DEPLOYMENT_NAME --replicas=$REPLICAS -n $NAMESPACE
        ;;
        
    *)
        echo "❌ Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac

if [[ "$ACTION" == "deploy" || "$ACTION" == "restart" || "$ACTION" == "rebuild" ]]; then
    echo ""
    echo "✅ $SERVICE_NAME $ACTION completed!"
    echo ""
    echo "📋 Quick commands:"
    echo "   Status: $0 $SERVICE_NAME status"
    echo "   Logs:   $0 $SERVICE_NAME logs"
    echo "   Port:   kubectl port-forward svc/$DEPLOYMENT_NAME $PORT:$PORT -n $NAMESPACE"
fi