# k8s/scripts/deploy.sh

echo "Deploying Barakah Fund to Docker Desktop Kubernetes..."

if ! kubectl cluster-info &> /dev/null; then
    echo "Docker Desktop Kubernetes is not running"
    echo "Please enable Kubernetes in Docker Desktop settings"
    echo "Docker Desktop > Settings > Kubernetes > Enable Kubernetes"
    exit 1
fi

echo "Docker Desktop Kubernetes is running"

PROJECT_ROOT="/Users/mfachryna/Documents/kodingan/BSI/ojt_spesifik/barakah-fund"
K8S_DIR="$PROJECT_ROOT/k8s"

echo "Creating namespaces..."
kubectl apply -f $K8S_DIR/infrastructure/namespace.yaml

echo "Applying RBAC"
kubectl apply -f $K8S_DIR/infrastructure/rbac.yaml
echo "Applying ConfigMaps and Secrets..."
kubectl apply -f $K8S_DIR/infrastructure/configmaps.yaml
kubectl apply -f $K8S_DIR/infrastructure/secrets.yaml

echo "Creating Keycloak realm configuration..."
kubectl apply -f $K8S_DIR/infrastructure/keycloak-realm.yaml

echo "Creating Persistent Volumes..."
kubectl apply -f $K8S_DIR/infrastructure/persistent-volumes.yaml

echo "Deploying infrastructure services..."
kubectl apply -f $K8S_DIR/infrastructure/postgres.yaml

echo "Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n barakah-fund --timeout=300s
if [ $? -eq 0 ]; then
    echo "PostgreSQL is ready!"
else
    echo "PostgreSQL failed to start. Check logs:"
    kubectl logs deployment/postgres -n barakah-fund
    exit 1
fi

echo "Deploying Keycloak with realm import..."
kubectl apply -f $K8S_DIR/infrastructure/keycloak.yaml

echo "Waiting for Keycloak to be ready..."
kubectl wait --for=condition=ready pod -l app=keycloak -n barakah-fund --timeout=600s
if [ $? -eq 0 ]; then
    echo "Keycloak is ready!"
    echo "Access Keycloak: kubectl port-forward service/keycloak-service 8080:8080 -n barakah-fund"
    echo "Then open: http://localhost:8080"
    echo "Admin credentials: admin / admin123"
else
    echo "Keycloak failed to start. Check logs:"
    kubectl logs deployment/keycloak -n barakah-fund
fi

echo "Deploying remaining infrastructure..."
kubectl apply -f $K8S_DIR/infrastructure/redis.yaml
kubectl apply -f $K8S_DIR/infrastructure/kafka.yaml

echo "Deploying services..."
kubectl apply -f $K8S_DIR/services/

echo "Deployment Summary:"
echo "==================="
kubectl get pods -n barakah-fund

echo ""
echo "Services:"
kubectl get services -n barakah-fund

echo ""
echo "To access Keycloak:"
echo "kubectl port-forward service/keycloak-service 8080:8080 -n barakah-fund"
echo ""
echo "To cleanup: ./k8s/scripts/cleanup.sh"