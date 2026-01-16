# Workflow Engine Helm Chart

Helm chart para deploy do Workflow Engine no Kubernetes.

## 📋 Pré-requisitos

- Kubernetes 1.19+
- Helm 3.0+
- Kubectl configurado
- Acesso ao registry: `registry.digitalocean.com/saui`

## 🚀 Quick Start

### 1. Build e Push da Imagem

```bash
cd /home/joaopedro/workflow-engine
./scripts/build-image.sh
```

### 2. Deploy Development

```bash
helm upgrade --install workflow-engine ./charts \
  --namespace apporte-workflow-dev --create-namespace \
  --values charts/values-dev.yaml \
  --set secrets.database.password="YOUR_DB_PASSWORD" \
  --set secrets.keycloak.clientSecret="YOUR_KEYCLOAK_SECRET" \
  --wait --timeout 5m
```

### 3. Deploy Production

```bash
helm upgrade --install workflow-engine ./charts \
  --namespace apporte-workflow-prod --create-namespace \
  --values charts/values-prod.yaml \
  --set secrets.database.password="YOUR_DB_PASSWORD" \
  --set secrets.keycloak.clientSecret="YOUR_KEYCLOAK_SECRET" \
  --wait --timeout 5m
```

## 📁 Estrutura do Chart

```
charts/
├── Chart.yaml              # Metadata do chart
├── values.yaml             # Valores padrão
├── values-dev.yaml         # Overrides para dev
├── values-prod.yaml        # Overrides para prod
└── templates/
    ├── _helpers.tpl        # Template helpers
    ├── deployment.yaml     # Deployment do app
    ├── service.yaml        # Service ClusterIP
    ├── configmap.yaml      # ConfigMap com env vars
    ├── secret.yaml         # Secret com credenciais
    ├── ingress.yaml        # Ingress (opcional)
    ├── pvc.yaml            # PVC (opcional)
    ├── serviceaccount.yaml # ServiceAccount
    └── hpa.yaml            # HPA (opcional)
```

## ⚙️ Configuração

### Variáveis de Ambiente

Todas as variáveis não-sensíveis estão no ConfigMap:

- `APP_ENV`: Ambiente (development/production)
- `DB_HOST`: Host do PostgreSQL
- `DB_PORT`: Porta do banco (5432)
- `DB_NAME`: Nome do database
- `DB_USERNAME`: Usuário do banco
- `KEYCLOAK_AUTH_SERVER_URL`: URL do Keycloak
- `KEYCLOAK_CLIENT_ID`: Client ID do Keycloak
- `CORS_ORIGINS`: Origens permitidas

### Secrets

Secrets são injetados via `--set` flags:

```bash
--set secrets.database.password="senha_secreta"
--set secrets.keycloak.clientSecret="client_secret"
```

**⚠️ NUNCA commite valores reais de secrets!**

### Uso de Secret Externo

Se você gerencia secrets externamente (Sealed Secrets, External Secrets Operator):

```yaml
# values.yaml
existingSecret: "workflow-engine-external-secrets"
```

Nesse caso, o Secret deve conter as keys:
- `DB_PASSWORD`
- `KEYCLOAK_CLIENT_SECRET`

## 🔧 Customização

### Resources

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### Autoscaling (Produção)

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 5
  targetCPUUtilizationPercentage: 70
```

### Ingress

```yaml
ingress:
  enabled: true
  className: nginx
  hosts:
    - host: api.apporte.work
      paths:
        - path: /api/workflow
          pathType: Prefix
  tls:
    - secretName: workflow-engine-tls
      hosts:
        - api.apporte.work
```

### Persistence (Opcional)

Para logs persistentes:

```yaml
persistence:
  enabled: true
  mountPath: /deployments/logs
  size: 10Gi
  storageClass: "do-block-storage"
```

## 🧪 Validação

### Lint do Chart

```bash
helm lint charts/
```

### Template Dry-Run

```bash
helm template workflow-engine ./charts \
  --values charts/values-dev.yaml \
  --set secrets.database.password="test" \
  --set secrets.keycloak.clientSecret="test"
```

### Install Dry-Run

```bash
helm install workflow-engine-test ./charts \
  --namespace apporte-workflow-dev --create-namespace \
  --values charts/values-dev.yaml \
  --set secrets.database.password="test" \
  --set secrets.keycloak.clientSecret="test" \
  --dry-run --debug
```

## 📊 Verificação Pós-Deploy

### Verificar Status

```bash
# Pods
kubectl get pods -n apporte-workflow-dev

# Deployment
kubectl get deployment -n apporte-workflow-dev

# Service
kubectl get svc -n apporte-workflow-dev

# Ingress
kubectl get ingress -n apporte-workflow-dev
```

### Logs

```bash
kubectl logs -n apporte-workflow-dev -l app.kubernetes.io/name=workflow-engine
```

### Health Checks

```bash
# Port-forward
kubectl port-forward -n apporte-workflow-dev svc/workflow-engine 8080:8080

# Test endpoints
curl http://localhost:8080/q/health/live
curl http://localhost:8080/q/health/ready
```

## 🔄 Atualizações

### Atualizar Imagem

```bash
# 1. Build nova imagem
./scripts/build-image.sh

# 2. Update deployment
helm upgrade workflow-engine ./charts \
  --namespace apporte-workflow-dev \
  --values charts/values-dev.yaml \
  --set image.tag="v1.0.1" \
  --set secrets.database.password="..." \
  --set secrets.keycloak.clientSecret="..." \
  --wait
```

### Atualizar Configuração

```bash
# Editar values-dev.yaml ou values-prod.yaml
vim charts/values-dev.yaml

# Apply changes
helm upgrade workflow-engine ./charts \
  --namespace apporte-workflow-dev \
  --values charts/values-dev.yaml \
  --reuse-values \
  --wait
```

### Rollback

```bash
# Ver histórico
helm history workflow-engine -n apporte-workflow-dev

# Rollback para revisão anterior
helm rollback workflow-engine -n apporte-workflow-dev
```

## 🗑️ Remoção

```bash
# Uninstall
helm uninstall workflow-engine -n apporte-workflow-dev

# Delete namespace (opcional)
kubectl delete namespace apporte-workflow-dev
```

## 🔐 Segurança

### Best Practices

1. **Nunca** commite secrets em values.yaml
2. Use `--set` flags ou external secret management
3. Habilite RBAC com ServiceAccount
4. Configure Network Policies se necessário
5. Use TLS para ingress
6. Configure Pod Security Context

### Exemplo com GitHub Actions

```yaml
- name: Deploy to Kubernetes
  run: |
    helm upgrade --install workflow-engine ./charts \
      --namespace apporte-workflow-dev --create-namespace \
      --values charts/values-dev.yaml \
      --set secrets.database.password="${{ secrets.DB_PASSWORD }}" \
      --set secrets.keycloak.clientSecret="${{ secrets.KEYCLOAK_CLIENT_SECRET }}" \
      --wait --timeout 5m
```

## 📚 Referências

- [Helm Documentation](https://helm.sh/docs/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Quarkus Kubernetes Guide](https://quarkus.io/guides/deploying-to-kubernetes)

## 🐛 Troubleshooting

### Pod não inicia

```bash
# Ver eventos
kubectl describe pod -n apporte-workflow-dev <pod-name>

# Ver logs
kubectl logs -n apporte-workflow-dev <pod-name>
```

### ImagePullBackOff

```bash
# Verificar secret de registry
kubectl get secrets -n apporte-workflow-dev

# Criar se necessário
kubectl create secret docker-registry regcred \
  --docker-server=registry.digitalocean.com \
  --docker-username=<token> \
  --docker-password=<token> \
  -n apporte-workflow-dev
```

### Health Check Failing

```bash
# Check liveness probe
kubectl exec -n apporte-workflow-dev <pod-name> -- curl localhost:8080/q/health/live

# Check readiness probe
kubectl exec -n apporte-workflow-dev <pod-name> -- curl localhost:8080/q/health/ready
```

### Database Connection Issues

```bash
# Verificar secrets
kubectl get secret workflow-engine-secrets -n apporte-workflow-dev -o yaml

# Test database connection
kubectl exec -n apporte-workflow-dev <pod-name> -- \
  psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME -c "SELECT 1"
```
