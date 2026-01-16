#!/bin/bash
set -e

# ========================================
# Local Deploy Script - Development
# ========================================
# Deploy directly to cluster using local .envrc secrets
# NO GitHub Actions required!

NAMESPACE="apporte-workflow-dev"
RELEASE_NAME="workflow-engine"
CHART_PATH="./charts"

echo "🚀 Local Deploy to Development Cluster"
echo ""

# Check if .envrc exists
if [ ! -f ".envrc" ]; then
    echo "❌ Error: .envrc file not found!"
    echo "Create .envrc from .envrc.example and source it first."
    exit 1
fi

# Source environment variables
echo "📋 Loading secrets from .envrc..."
source .envrc

# Validate required secrets
if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: DB_PASSWORD not set in .envrc"
    exit 1
fi

KEYCLOAK_SECRET="${KEYCLOAK_CLIENT_SECRET}"
if [ -z "$KEYCLOAK_SECRET" ]; then
    echo "❌ Error: KEYCLOAK_CLIENT_SECRET não está setado no .envrc"
    exit 1
fi

echo "✅ Secrets loaded"
echo ""

# Check cluster connection
echo "🔍 Checking cluster connection..."
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Error: Cannot connect to Kubernetes cluster"
    echo "Run: doctl kubernetes cluster kubeconfig save saui-k8s"
    exit 1
fi

CURRENT_CONTEXT=$(kubectl config current-context)
echo "✅ Connected to: $CURRENT_CONTEXT"
echo ""

# Optional: Build and push image first
read -p "Build and push new Docker image? (y/N): " BUILD_IMAGE
if [[ "$BUILD_IMAGE" =~ ^[Yy]$ ]]; then
    echo ""
    echo "🐳 Building Docker image..."
    ./mvnw clean package -DskipTests
    ./scripts/build-image.sh
    echo ""
fi

# Get image tag
read -p "Image tag to deploy (default: latest): " IMAGE_TAG
IMAGE_TAG=${IMAGE_TAG:-latest}

echo ""
echo "📦 Deploying with Helm..."
echo "  Release: $RELEASE_NAME"
echo "  Namespace: $NAMESPACE"
echo "  Image tag: $IMAGE_TAG"
echo ""

# Deploy with Helm
helm upgrade --install "$RELEASE_NAME" "$CHART_PATH" \
  --namespace "$NAMESPACE" --create-namespace \
  --values "$CHART_PATH/values-dev.yaml" \
  --set image.tag="$IMAGE_TAG" \
  --set secrets.database.password="$DB_PASSWORD" \
  --set secrets.keycloak.clientSecret="$KEYCLOAK_SECRET" \
  --wait --timeout 5m

echo ""
echo "✅ Deployment complete!"
echo ""

# Show status
echo "📊 Deployment Status:"
kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=workflow-engine
echo ""
kubectl get svc -n "$NAMESPACE" -l app.kubernetes.io/name=workflow-engine

echo ""
echo "🎉 Success! Your app is deployed."
echo ""
echo "Next steps:"
echo "  • View logs:        make logs-dev"
echo "  • Port-forward:     make port-forward-dev"
echo "  • Health check:     make health-dev"
echo "  • Shell into pod:   make shell-dev"
