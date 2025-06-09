
#!/bin/bash
set -e

SERVICE_NAME=$1
ACTION=${2:-deploy}

if [ -z "$SERVICE_NAME" ]; then
    echo "Usage: $0 <service-name> [action]"
    echo ""
    echo "Services: eureka, gateway, user, account, transaction, notification"
    echo "Actions: deploy, restart, rebuild (default: deploy)"
    echo ""
    echo "Examples:"
    echo "  $0 gateway              # Deploy gateway service"
    echo "  $0 user restart         # Restart user service"
    echo "  $0 transaction rebuild  # Rebuild and deploy transaction service"
    exit 1
fi

NAMESPACE="barakah-fund"
PROJECT_ROOT="/Users/mfachryna/Documents/kodingan/BSI/ojt_spesifik/barakah-fund"

# Service configurations
declare -A SERVICES
SERVICES["eureka"]="eureka-server:k8s/dockerfiles/eureka.Dockerfile:eureka-server"
SERVICES["gateway"]="gateway-service:k8s/dockerfiles/gateway.Dockerfile:gateway-service"
SERVICES["user"]="user-service:k8s/dockerfiles/user.Dockerfile:user-service"
SERVICES["account"]="account-service:k8s/dockerfiles/account.Dockerfile:account-service"
SERVICES["transaction"]="transaction-service:k8s/dockerfiles/transaction.Dockerfile:transaction-service"
SERVICES["notification"]="notification-service:k8s/dockerfiles/notification.Dockerfile:notification-service"

if [ -z "${SERVICES[$SERVICE_NAME]}" ]; then
    echo "❌ Unknown service: $SERVICE_NAME"
    echo "Available services: ${!SERVICES[@]}"
    exit 1
fi

IFS=':' read -r IMAGE_NAME DOCKERFILE DEPLOYMENT_NAME <<< "${SERVICES[$SERVICE_NAME]}"

echo "🎯 Processing $SERVICE_NAME ($ACTION)..."
echo "=================================="
echo "Image: barakah/$IMAGE_NAME"
echo "Dockerfile: $DOCKERFILE"
echo "Deployment: $DEPLOYMENT_NAME"
echo ""

cd "$PROJECT_ROOT"

case $ACTION in
    "rebuild")
        echo "🔨 Rebuilding image..."
        docker build -f "$DOCKERFILE" -t "barakah/$IMAGE_NAME:latest" .
        
        echo "🔄 Restarting deployment..."
        kubectl rollout restart deployment/$DEPLOYMENT_NAME -n $NAMESPACE
        kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE
        ;;
        
    "restart")
        echo "🔄 Restarting deployment..."
        kubectl rollout restart deployment/$DEPLOYMENT_NAME -n $NAMESPACE
        kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE
        ;;
        
    "deploy")
        echo "🚀 Deploying service..."
        kubectl apply -f "k8s/services/${SERVICE_NAME}-service.yaml" -n $NAMESPACE
        kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE
        ;;
        
    *)
        echo "❌ Unknown action: $ACTION"
        echo "Available actions: deploy, restart, rebuild"
        exit 1
        ;;
esac

echo ""
echo "✅ $SERVICE_NAME $ACTION completed!"
echo ""
echo "📋 Status:"
kubectl get pods -l app=$DEPLOYMENT_NAME -n $NAMESPACE
echo ""
echo "📜 Logs: kubectl logs -l app=$DEPLOYMENT_NAME -n $NAMESPACE -f"
echo "🔗 Port forward: kubectl port-forward svc/$DEPLOYMENT_NAME <local-port>:<service-port> -n $NAMESPACE"