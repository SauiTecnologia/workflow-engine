#!/bin/bash
set -euo pipefail

# ============================================
# Build and Push Workflow Engine Docker Image
# ============================================

# Configuration
REGISTRY="${DOCKER_REGISTRY:-registry.digitalocean.com/saui-main-registry}"
IMAGE_NAME="workflow-engine"
VERSION="${VERSION:-$(git rev-parse --short HEAD 2>/dev/null || echo 'latest')}"
FULL_IMAGE="${REGISTRY}/${IMAGE_NAME}:${VERSION}"
LATEST_IMAGE="${REGISTRY}/${IMAGE_NAME}:latest"

echo "============================================"
echo "🐳 Building Workflow Engine Docker Image"
echo "============================================"
echo "Registry: ${REGISTRY}"
echo "Image: ${IMAGE_NAME}"
echo "Version: ${VERSION}"
echo "Full tag: ${FULL_IMAGE}"
echo ""

# Build the application JAR
echo "1️⃣ Building application with Maven..."
./mvnw clean package -DskipTests -B

# Build Docker image
echo ""
echo "2️⃣ Building Docker image..."
docker build -t "${FULL_IMAGE}" -t "${LATEST_IMAGE}" -f src/main/docker/Dockerfile.jvm .

# Get image size
IMAGE_SIZE=$(docker images "${FULL_IMAGE}" --format "{{.Size}}")
echo ""
echo "✅ Image built successfully!"
echo "   Size: ${IMAGE_SIZE}"
echo ""

# Optional: Push to registry
read -p "Push to registry? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "3️⃣ Pushing to registry..."
    
    # Login to DigitalOcean registry
    if [ -n "${DIGITALOCEAN_TOKEN:-}" ]; then
        echo "${DIGITALOCEAN_TOKEN}" | docker login registry.digitalocean.com -u "${DIGITALOCEAN_TOKEN}" --password-stdin
    fi
    
    docker push "${FULL_IMAGE}"
    docker push "${LATEST_IMAGE}"
    
    echo ""
    echo "✅ Image pushed successfully!"
    echo "   ${FULL_IMAGE}"
    echo "   ${LATEST_IMAGE}"
fi

echo ""
echo "============================================"
echo "🎉 Done!"
echo "============================================"
echo ""
echo "To deploy to Kubernetes:"
echo "  cd /home/joaopedro/iac/stacks/050-workflow-engine"
echo "  pulumi config set image ${FULL_IMAGE}"
echo "  pulumi up"
echo ""
