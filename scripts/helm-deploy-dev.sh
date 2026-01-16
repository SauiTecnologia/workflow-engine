#!/bin/bash
set -e

# ========================================
# Helm Deploy Script - Development
# ========================================

NAMESPACE="apporte-workflow-dev"
RELEASE_NAME="workflow-engine"
CHART_PATH="./charts"

echo "🚀 Deploying Workflow Engine to Development..."

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

# Lint chart
echo "🔍 Linting Helm chart..."
helm lint "$CHART_PATH"

# Template validation (dry-run)
echo "📋 Validating Helm templates..."
helm template "$RELEASE_NAME" "$CHART_PATH" \
  --values "$CHART_PATH/values-dev.yaml" \
  --set secrets.database.password="$DB_PASSWORD" \
  --set secrets.keycloak.clientSecret="$CLIENT_SECRET" \
  > /dev/null

# Deploy/Upgrade
echo "📦 Deploying to Kubernetes..."
helm upgrade --install "$RELEASE_NAME" "$CHART_PATH" \
  --namespace "$NAMESPACE" --create-namespace \
  --values "$CHART_PATH/values-dev.yaml" \
  --set image.tag="$IMAGE_TAG" \
  --set secrets.database.password="$DB_PASSWORD" \
  --set secrets.keycloak.clientSecret="$CLIENT_SECRET" \
  --wait --timeout 5m

# Verify deployment
echo "✅ Verifying deployment..."
kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=workflow-engine

echo ""
echo "✨ Deployment complete!"
echo ""
echo "Check status:"
echo "  kubectl get pods -n $NAMESPACE"
echo "  kubectl logs -n $NAMESPACE -l app.kubernetes.io/name=workflow-engine"
echo ""
echo "Port-forward to test locally:"
echo "  kubectl port-forward -n $NAMESPACE svc/workflow-engine 8080:8080"
echo ""
echo "Test health:"
echo "  curl http://localhost:8080/q/health/live"
echo "  curl http://localhost:8080/q/health/ready"
