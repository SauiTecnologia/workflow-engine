#!/bin/bash
set -e

# ========================================
# Helm Deploy Script - Production
# ========================================

NAMESPACE="apporte-workflow-prod"
RELEASE_NAME="workflow-engine"
CHART_PATH="./charts"

echo "🚀 Deploying Workflow Engine to Production..."

# Production safety checks
echo "⚠️  WARNING: Deploying to PRODUCTION!"
echo ""
read -p "Are you sure you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ Deployment cancelled."
    exit 0
fi

# Check if .envrc exists
if [ ! -f ".envrc" ]; then
    echo "❌ Error: .envrc file not found!"
    echo "Create .envrc from .envrc.example and source it first."
    exit 1
fi

# Source environment variables
source .envrc

# Validate required secrets
if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: DB_PASSWORD not set in .envrc"; exit 1;
fi
if [ -z "$OIDC_CLIENT_SECRET" ] && [ -z "$KEYCLOAK_CLIENT_SECRET" ]; then
    echo "❌ Error: KEYCLOAK_CLIENT_SECRET não está setado em .envrc"; exit 1;
fi
CLIENT_SECRET="${OIDC_CLIENT_SECRET:-$KEYCLOAK_CLIENT_SECRET}"

# Validate image tag
if [ -z "$IMAGE_TAG" ]; then
    echo "❌ Error: IMAGE_TAG not set!"
    echo "Set IMAGE_TAG environment variable (e.g., v1.0.0)"
    exit 1
fi

# Lint chart
echo "🔍 Linting Helm chart..."
helm lint "$CHART_PATH"

# Template validation (dry-run)
echo "📋 Validating Helm templates..."
helm template "$RELEASE_NAME" "$CHART_PATH" \
    --values "$CHART_PATH/values-prod.yaml" \
    --set image.tag="$IMAGE_TAG" \
    --set secrets.database.password="$DB_PASSWORD" \
    --set secrets.keycloak.clientSecret="$CLIENT_SECRET" \
    > /dev/null

echo ""
echo "About to deploy:"
echo "  Image: registry.digitalocean.com/saui-main-registry/workflow-engine:$IMAGE_TAG"
echo "  Namespace: $NAMESPACE"
echo ""
read -p "Proceed with deployment? (yes/no): " confirm2

if [ "$confirm2" != "yes" ]; then
    echo "❌ Deployment cancelled."
    exit 0
fi

# Deploy/Upgrade
echo "📦 Deploying to Kubernetes..."
helm upgrade --install "$RELEASE_NAME" "$CHART_PATH" \
        --namespace "$NAMESPACE" --create-namespace \
        --values "$CHART_PATH/values-prod.yaml" \
        --set image.tag="$IMAGE_TAG" \
        --set secrets.database.password="$DB_PASSWORD" \
        --set secrets.keycloak.clientSecret="$CLIENT_SECRET" \
    --wait --timeout 10m

# Verify deployment
echo "✅ Verifying deployment..."
kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=workflow-engine

# Check rollout status
kubectl rollout status deployment/workflow-engine -n "$NAMESPACE"

echo ""
echo "✨ Production deployment complete!"
echo ""
echo "Check status:"
echo "  kubectl get pods -n $NAMESPACE"
echo "  kubectl logs -n $NAMESPACE -l app.kubernetes.io/name=workflow-engine"
echo ""
echo "Monitor metrics:"
echo "  kubectl top pods -n $NAMESPACE"
echo ""
echo "View history:"
echo "  helm history $RELEASE_NAME -n $NAMESPACE"
echo ""
echo "Rollback if needed:"
echo "  helm rollback $RELEASE_NAME -n $NAMESPACE"
