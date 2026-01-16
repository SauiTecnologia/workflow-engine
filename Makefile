# ============================================
# Workflow Engine Makefile
# ============================================
# Simplified build and deployment orchestration

.DEFAULT_GOAL := help
.PHONY: help build test clean dev docker-build docker-push docker-run \
        deploy-dev deploy-prod deploy dry-run lint-chart template-chart \
        status-dev status-prod logs-dev logs-prod history-dev history-prod \
        rollback-dev rollback-prod destroy-dev destroy-prod \
        port-forward-dev port-forward-prod describe-dev describe-prod \
        events-dev events-prod health-dev health-prod shell-dev shell-prod \
        test-endpoints check-cluster check-keycloak restart-dev restart-prod \
        scale-dev scale-prod secrets-dev secrets-prod

# Helper functions for output
define title
	@echo "→ $(1)"
endef

define success
	@echo "✓ $(1)"
endef

define warning
	@echo "⚠ $(1)"
endef

define error
	@echo "✗ $(1)"
endef

define info
	@echo "ℹ $(1)"
endef

define banner
	@echo "================================================"
	@echo "  $(1)"
	@echo "================================================"
endef

# Configuration
NAMESPACE_DEV := apporte-workflow-dev
NAMESPACE_PROD := apporte-workflow-prod
APP_NAME := workflow-engine
CHART_PATH := ./charts
IMAGE_REGISTRY := registry.digitalocean.com/saui-main-registry
IMAGE_NAME := $(IMAGE_REGISTRY)/$(APP_NAME)
IMAGE_TAG ?= latest
PORT := 8080


# Check for required environment variables (aceita KEYCLOAK_CLIENT_SECRET como fallback)
REQUIRED_ENV_VARS := DB_PASSWORD
check-env:
	@if [ -z "$(DB_PASSWORD)" ]; then \
		echo "✗ DB_PASSWORD is not set. Source .envrc first"; exit 1; \
	fi
	@if [ -z "$(OIDC_CLIENT_SECRET)" ] && [ -z "$(KEYCLOAK_CLIENT_SECRET)" ]; then \
		echo "✗ OIDC_CLIENT_SECRET ou KEYCLOAK_CLIENT_SECRET não está setado. Source .envrc first"; exit 1; \
	fi

##@ Help

help: ## Show available commands
	@echo ""
	@$(call banner,Workflow Engine - Available Commands)
	@echo ""
	@awk 'BEGIN {FS = ":.*##"} \
		/^[a-zA-Z_-]+:.*?##/ { printf "  %-25s %s\n", $$1, $$2 } \
		/^##@/ { printf "\n%s\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
	@echo ""

##@ Development

dev: ## Start Quarkus in dev mode (hot reload)
	$(call title,Starting Quarkus dev mode with hot reload...)
	@./mvnw clean quarkus:dev

dev-debug: ## Start Quarkus in dev mode with debug enabled (port 5005)
	$(call title,Starting Quarkus dev mode with debug on port 5005...)
	@./mvnw clean quarkus:dev -Ddebug=5005

build: ## Build with Maven (skip tests)
	$(call title,Building with Maven...)
	@./mvnw clean package -DskipTests
	$(call success,Build complete)

test: ## Run all tests
	$(call title,Running tests...)
	@./mvnw test

test-integration: ## Run integration tests
	$(call title,Running integration tests...)
	@./mvnw verify -Pintegration-tests

clean: ## Clean build artifacts
	$(call title,Cleaning build artifacts...)
	@./mvnw clean
	@rm -rf target/
	$(call success,Clean complete)

format: ## Format code with Maven
	@./mvnw formatter:format

##@ Docker

docker-build: ## Build Docker image
	$(call title,Building Docker image...)
	@./scripts/build-image.sh
	$(call success,Docker image built: $(IMAGE_NAME):$(IMAGE_TAG))

docker-push: ## Push Docker image to registry
	$(call title,Pushing image to registry...)
	@docker push $(IMAGE_NAME):$(IMAGE_TAG)
	$(call success,Image pushed: $(IMAGE_NAME):$(IMAGE_TAG))

docker-run: ## Run container locally (port 8080)
	$(call title,Running container locally on port $(PORT)...)
	@docker run -p $(PORT):$(PORT) \
		--env-file .envrc \
		--name workflow-engine-local \
		--rm \
		$(IMAGE_NAME):$(IMAGE_TAG)

docker-stop: ## Stop local container
	@docker stop workflow-engine-local || true

docker-clean: ## Remove local Docker images
	$(call title,Cleaning Docker images...)
	@docker rmi $(IMAGE_NAME):$(IMAGE_TAG) || true
	@docker system prune -f

##@ Helm

lint-chart: ## Lint Helm chart
	$(call title,Linting Helm chart...)
	@helm lint $(CHART_PATH)
	$(call success,Chart is valid)

template-chart: ## Render Helm templates (dry-run)
	$(call title,Rendering Helm templates...)
	@helm template $(APP_NAME) $(CHART_PATH) \
		--values $(CHART_PATH)/values-dev.yaml \
		--set secrets.database.password="test" \
		--set secrets.keycloak.clientSecret="test"

dry-run: check-env ## Dry-run deployment (usage: make dry-run ENV=dev)
	$(call title,Dry-run deployment for $(ENV)...)
	@helm install $(APP_NAME)-test $(CHART_PATH) \
		--namespace $(NAMESPACE_$(shell echo $(ENV) | tr '[:lower:]' '[:upper:]')) \
		--values $(CHART_PATH)/values-$(ENV).yaml \
		--set secrets.database.password="$(DB_PASSWORD)" \
		--set secrets.keycloak.clientSecret="$(if $(OIDC_CLIENT_SECRET),$(OIDC_CLIENT_SECRET),$(KEYCLOAK_CLIENT_SECRET))" \
		--dry-run --debug

##@ Deployment

deploy-dev: check-env ## Deploy to development
	$(call banner,Deploying to Development)
	@./scripts/helm-deploy-dev.sh
	$(call success,Deployment complete)

deploy-prod: check-env ## Deploy to production
	$(call banner,Deploying to Production)
	@./scripts/helm-deploy-prod.sh
	$(call success,Deployment complete)

deploy: check-env ## Deploy to specific environment (usage: make deploy ENV=dev)
	$(call title,Deploying to $(ENV)...)
	@helm upgrade --install $(APP_NAME) $(CHART_PATH) \
		--namespace $(NAMESPACE_$(shell echo $(ENV) | tr '[:lower:]' '[:upper:]')) \
		--create-namespace \
		--values $(CHART_PATH)/values-$(ENV).yaml \
		--set image.tag=$(IMAGE_TAG) \
		--set secrets.database.password="$(DB_PASSWORD)" \
		--set secrets.keycloak.clientSecret="$(if $(OIDC_CLIENT_SECRET),$(OIDC_CLIENT_SECRET),$(KEYCLOAK_CLIENT_SECRET))" \
		--wait --timeout 5m
	$(call success,Deployed to $(ENV))

full-deploy-dev: check-env ## Build + Push + Deploy to dev
	$(call banner,Full deployment pipeline to Development)
	@$(MAKE) build
	@$(MAKE) docker-build
	@$(MAKE) docker-push
	@$(MAKE) deploy-dev
	$(call success,Full deployment complete!)

full-deploy-prod: check-env ## Build + Push + Deploy to prod
	$(call banner,Full deployment pipeline to Production)
	@$(MAKE) build
	@$(MAKE) docker-build IMAGE_TAG=$(IMAGE_TAG)
	@$(MAKE) docker-push IMAGE_TAG=$(IMAGE_TAG)
	@$(MAKE) deploy-prod
	$(call success,Full deployment complete!)

##@ Status & Monitoring

status-dev: ## Show development deployment status
	$(call title,Development Status)
	@kubectl get pods -n $(NAMESPACE_DEV) -l app.kubernetes.io/name=$(APP_NAME)
	@echo ""
	@kubectl get svc -n $(NAMESPACE_DEV) -l app.kubernetes.io/name=$(APP_NAME)

status-prod: ## Show production deployment status
	$(call title,Production Status)
	@kubectl get pods -n $(NAMESPACE_PROD) -l app.kubernetes.io/name=$(APP_NAME)
	@echo ""
	@kubectl get svc -n $(NAMESPACE_PROD) -l app.kubernetes.io/name=$(APP_NAME)

logs-dev: ## Stream development logs
	$(call title,Streaming development logs...)
	@kubectl logs -n $(NAMESPACE_DEV) -l app.kubernetes.io/name=$(APP_NAME) --tail=100 -f

logs-prod: ## Stream production logs
	$(call title,Streaming production logs...)
	@kubectl logs -n $(NAMESPACE_PROD) -l app.kubernetes.io/name=$(APP_NAME) --tail=100 -f

health-dev: ## Check development health endpoints
	$(call title,Checking development health...)
	@kubectl exec -n $(NAMESPACE_DEV) deployment/$(APP_NAME) -- curl -s localhost:$(PORT)/q/health/live | jq .
	@echo ""
	@kubectl exec -n $(NAMESPACE_DEV) deployment/$(APP_NAME) -- curl -s localhost:$(PORT)/q/health/ready | jq .

health-prod: ## Check production health endpoints
	$(call title,Checking production health...)
	@kubectl exec -n $(NAMESPACE_PROD) deployment/$(APP_NAME) -- curl -s localhost:$(PORT)/q/health/live | jq .
	@echo ""
	@kubectl exec -n $(NAMESPACE_PROD) deployment/$(APP_NAME) -- curl -s localhost:$(PORT)/q/health/ready | jq .

##@ Rollback & History

history-dev: ## Show development deployment history
	@helm history $(APP_NAME) -n $(NAMESPACE_DEV)

history-prod: ## Show production deployment history
	@helm history $(APP_NAME) -n $(NAMESPACE_PROD)

rollback-dev: ## Rollback development deployment
	$(call warning,Rolling back development...)
	@helm rollback $(APP_NAME) -n $(NAMESPACE_DEV)
	$(call success,Rollback complete)

rollback-prod: ## Rollback production deployment (with confirmation)
	$(call error,Rolling back production...)
	@read -p "Are you sure? [y/N]: " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		helm rollback $(APP_NAME) -n $(NAMESPACE_PROD); \
		echo "✓ Rollback complete"; \
	else \
		echo "⚠ Cancelled"; \
	fi

##@ Cleanup

destroy-dev: ## Destroy development deployment
	$(call warning,Destroying development deployment...)
	@helm uninstall $(APP_NAME) -n $(NAMESPACE_DEV)
	$(call success,Development destroyed)

destroy-prod: ## Destroy production deployment (with confirmation)
	$(call banner,WARNING: Destroying PRODUCTION)
	@read -p "Are you ABSOLUTELY sure? [y/N]: " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		helm uninstall $(APP_NAME) -n $(NAMESPACE_PROD); \
		echo "✓ Destroyed"; \
	else \
		echo "⚠ Cancelled"; \
	fi

##@ Local Access

port-forward-dev: ## Port-forward dev app to localhost:8080
	$(call title,Port-forwarding development app)
	@echo "Access app at: http://localhost:$(PORT)"
	@echo "Health: http://localhost:$(PORT)/q/health"
	@echo "Metrics: http://localhost:$(PORT)/q/metrics"
	@echo "Press Ctrl+C to stop"
	@echo ""
	@kubectl port-forward -n $(NAMESPACE_DEV) svc/$(APP_NAME) $(PORT):$(PORT)

port-forward-prod: ## Port-forward prod app to localhost:8081
	$(call title,Port-forwarding production app)
	@echo "Access app at: http://localhost:8081"
	@echo "Health: http://localhost:8081/q/health"
	@echo "Metrics: http://localhost:8081/q/metrics"
	@echo "Press Ctrl+C to stop"
	@echo ""
	@kubectl port-forward -n $(NAMESPACE_PROD) svc/$(APP_NAME) 8081:$(PORT)

##@ Debugging

describe-dev: ## Describe development pods in detail
	$(call title,Describing development pods)
	@kubectl describe pods -n $(NAMESPACE_DEV) -l app.kubernetes.io/name=$(APP_NAME)

describe-prod: ## Describe production pods in detail
	$(call title,Describing production pods)
	@kubectl describe pods -n $(NAMESPACE_PROD) -l app.kubernetes.io/name=$(APP_NAME)

events-dev: ## Show development namespace events
	$(call title,Development namespace events)
	@kubectl get events -n $(NAMESPACE_DEV) --sort-by='.lastTimestamp'

events-prod: ## Show production namespace events
	$(call title,Production namespace events)
	@kubectl get events -n $(NAMESPACE_PROD) --sort-by='.lastTimestamp'

shell-dev: ## Open shell in development pod
	$(call title,Opening shell in development pod...)
	@kubectl exec -it -n $(NAMESPACE_DEV) deployment/$(APP_NAME) -- /bin/bash

shell-prod: ## Open shell in production pod
	$(call title,Opening shell in production pod...)
	@kubectl exec -it -n $(NAMESPACE_PROD) deployment/$(APP_NAME) -- /bin/bash

restart-dev: ## Restart development deployment
	$(call title,Restarting development deployment...)
	@kubectl rollout restart deployment/$(APP_NAME) -n $(NAMESPACE_DEV)
	@kubectl rollout status deployment/$(APP_NAME) -n $(NAMESPACE_DEV)
	$(call success,Development restarted)

restart-prod: ## Restart production deployment
	$(call title,Restarting production deployment...)
	@kubectl rollout restart deployment/$(APP_NAME) -n $(NAMESPACE_PROD)
	@kubectl rollout status deployment/$(APP_NAME) -n $(NAMESPACE_PROD)
	$(call success,Production restarted)

##@ Scaling

scale-dev: ## Scale development (usage: make scale-dev REPLICAS=3)
	$(call title,Scaling development to $(REPLICAS) replicas...)
	@kubectl scale deployment/$(APP_NAME) -n $(NAMESPACE_DEV) --replicas=$(REPLICAS)
	$(call success,Scaled to $(REPLICAS) replicas)

scale-prod: ## Scale production (usage: make scale-prod REPLICAS=5)
	$(call title,Scaling production to $(REPLICAS) replicas...)
	@kubectl scale deployment/$(APP_NAME) -n $(NAMESPACE_PROD) --replicas=$(REPLICAS)
	$(call success,Scaled to $(REPLICAS) replicas)

##@ Secrets

secrets-dev: ## Show development secrets (base64 decoded)
	$(call title,Development Secrets)
	@kubectl get secret $(APP_NAME)-secrets -n $(NAMESPACE_DEV) -o jsonpath='{.data}' | jq 'map_values(@base64d)'

secrets-prod: ## Show production secrets (base64 decoded)
	$(call title,Production Secrets)
	@kubectl get secret $(APP_NAME)-secrets -n $(NAMESPACE_PROD) -o jsonpath='{.data}' | jq 'map_values(@base64d)'

##@ Testing

test-endpoints: ## Test application endpoints (dev and prod)
	$(call title,Testing application endpoints)
	@echo "DEV (via port-forward):"
	@curl -s http://localhost:$(PORT)/q/health/live || echo "❌ Development not accessible via port-forward"
	@echo ""
	@echo "PROD:"
	@curl -s https://api.apporte.work/api/workflow/q/health/live || echo "❌ Production not accessible"

check-keycloak: ## Test Keycloak connection
	$(call title,Testing Keycloak connection...)
	@./test-keycloak.sh || echo "❌ Keycloak test failed"

check-cluster: ## Check Kubernetes cluster connection and status
	$(call title,Checking Kubernetes cluster)
	@kubectl cluster-info
	@echo ""
	@echo "Nodes:"
	@kubectl get nodes
	@echo ""
	@echo "Namespaces:"
	@kubectl get namespaces | grep -E "(apporte|nginx|observability)"
	@echo ""
	@echo "Current context:"
	@kubectl config current-context

##@ CI/CD

ci-build: ## CI build (no cache)
	$(call title,CI Build - No Cache)
	@./mvnw clean package -DskipTests -B

ci-test: ## CI test with coverage
	$(call title,CI Test with Coverage)
	@./mvnw clean test jacoco:report -B

ci-docker: ## CI Docker build and push
	$(call title,CI Docker Build & Push)
	@docker build -t $(IMAGE_NAME):$(IMAGE_TAG) -f src/main/docker/Dockerfile.jvm .
	@docker push $(IMAGE_NAME):$(IMAGE_TAG)

##@ Quick Commands

quick-dev: ## Quick deploy to dev (assumes image already built)
	@$(MAKE) deploy-dev

quick-prod: ## Quick deploy to prod (assumes image already built)
	@$(MAKE) deploy-prod

quick-logs: ## Quick logs from dev
	@$(MAKE) logs-dev

quick-status: ## Quick status of both environments
	@$(MAKE) status-dev
	@echo ""
	@$(MAKE) status-prod

quick-restart: ## Quick restart dev
	@$(MAKE) restart-dev
