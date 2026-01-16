# CI/CD Quick Guide

## 🚀 Como funciona

### Triggers Automáticos

```bash
# Development (automático)
git push origin develop
→ Build → Test → Deploy to dev

# Production (automático)
git push origin main
→ Build → Test → Deploy to prod

# Manual (via GitHub Actions UI)
Actions → CI/CD → Run workflow → Choose environment
```

---

## 📋 Workflow Steps

### 1️⃣ Test (10 min)
- ✅ Checkout código
- ✅ Setup JDK 21
- ✅ Rodar testes Maven
- ✅ Gerar relatório de cobertura
- ✅ Upload artefatos

### 2️⃣ Build & Push (20 min)
- ✅ Build Maven (package)
- ✅ Build Docker image (Quarkus JVM)
- ✅ Push para DigitalOcean registry
- ✅ Scan de vulnerabilidades (Trivy)
- ✅ Tag: commit SHA, environment, latest

### 3️⃣ Deploy (15 min)
- ✅ Setup kubectl (via doctl)
- ✅ Validar Helm chart
- ✅ Deploy com Helm
- ✅ Wait for rollout
- ✅ Health checks
- ✅ Smoke tests
- ✅ Rollback automático se falhar

---

## 🎯 Cenários Comuns

### Deploy para Development

```bash
# Opção 1: Push para develop
git checkout develop
git add .
git commit -m "feat: nova funcionalidade"
git push origin develop

# Opção 2: Manual via GitHub UI
# Actions > CI/CD > Run workflow > environment: dev
```

**Resultado**: Deploy em `apporte-workflow-dev`

### Deploy para Production

```bash
# Opção 1: Push para main (após merge de PR)
git checkout main
git merge develop
git push origin main

# Opção 2: Tag de versão
git tag v1.0.0
git push origin v1.0.0

# Opção 3: Manual via GitHub UI
# Actions > CI/CD > Run workflow > environment: prod
```

**Resultado**: Deploy em `apporte-workflow-prod`

### Build sem Deploy

```bash
# Via GitHub UI:
# Actions > CI/CD > Run workflow
# ✅ skip_deployment: true
```

**Uso**: Testar build/testes sem afetar cluster

### Skip Tests (emergência)

```bash
# Via GitHub UI:
# Actions > CI/CD > Run workflow
# ✅ skip_tests: true
```

**⚠️ Use apenas em emergências**

---

## 📊 Monitorar Deploy

### Via GitHub Actions UI

```
1. Acesse: https://github.com/YOUR_ORG/workflow-engine/actions
2. Clique no workflow em execução
3. Expanda cada step para ver logs detalhados
4. Verifique status: ✅ Success / ❌ Failed
```

### Via Terminal (tempo real)

```bash
# Monitorar pods
watch kubectl get pods -n apporte-workflow-dev

# Seguir logs do deploy
kubectl logs -f -n apporte-workflow-dev -l app.kubernetes.io/name=workflow-engine

# Ver eventos
kubectl get events -n apporte-workflow-dev --sort-by='.lastTimestamp'
```

### Via Makefile

```bash
# Status rápido
make status-dev
make status-prod

# Logs em tempo real
make logs-dev
make logs-prod

# Health check
make health-dev
make health-prod
```

---

## 🔄 Rollback

### Automático (no CI/CD)

Se health checks ou smoke tests falharem, rollback é automático:

```yaml
- name: Rollback on failure
  if: failure()
  run: helm rollback workflow-engine
```

### Manual (via GitHub)

```bash
# 1. Acesse Actions no GitHub
# 2. Clique no deploy anterior que funcionou
# 3. Re-run workflow
```

### Manual (via Terminal)

```bash
# Ver histórico
make history-dev

# Rollback para revisão anterior
make rollback-dev

# Ou via Helm:
helm rollback workflow-engine -n apporte-workflow-dev
```

---

## ⚡ Comandos Rápidos

### Verificar último deploy

```bash
# Via Helm
helm list -n apporte-workflow-dev
helm history workflow-engine -n apporte-workflow-dev

# Via kubectl
kubectl rollout status deployment/workflow-engine -n apporte-workflow-dev
kubectl get pods -n apporte-workflow-dev
```

### Forçar re-deploy

```bash
# Opção 1: Via GitHub (re-run)
# Actions > Select workflow > Re-run jobs

# Opção 2: Via Makefile
make restart-dev
make restart-prod

# Opção 3: Via kubectl
kubectl rollout restart deployment/workflow-engine -n apporte-workflow-dev
```

### Debug deployment

```bash
# Logs do pod
kubectl logs -n apporte-workflow-dev deployment/workflow-engine --tail=100

# Describe deployment
kubectl describe deployment workflow-engine -n apporte-workflow-dev

# Describe pod
kubectl describe pod -n apporte-workflow-dev -l app.kubernetes.io/name=workflow-engine

# Shell no pod
kubectl exec -it -n apporte-workflow-dev deployment/workflow-engine -- bash
```

---

## 🐛 Troubleshooting

### ❌ Tests Failed

```bash
# Ver logs no GitHub Actions
# Corrigir código
# Push novamente

# Pular testes em emergência (não recomendado):
# Actions > Run workflow > skip_tests: true
```

### ❌ Docker Build Failed

```bash
# Comum: target/ não gerado
# Solução: ./mvnw clean package -DskipTests

# Comum: Dockerfile não encontrado
# Solução: Verificar src/main/docker/Dockerfile.jvm existe
```

### ❌ Helm Deploy Failed

```bash
# Ver logs detalhados no GitHub Actions

# Comum: Secrets não configurados
# Solução: Verificar GitHub Secrets (Settings > Secrets)

# Comum: Namespace não existe
# Solução: Helm cria automaticamente com --create-namespace
```

### ❌ Health Check Failed

```bash
# Pod não iniciou a tempo
# Solução: Aumentar timeout em values.yaml (initialDelaySeconds)

# Banco de dados inacessível
# Solução: Verificar DB_PASSWORD no secret

# Keycloak inacessível
# Solução: Verificar KEYCLOAK_CLIENT_SECRET no secret
```

### ❌ Smoke Test Failed

```bash
# Geralmente não crítico (continue-on-error: true)
# Apenas alerta no log
# Deploy continua
```

---

## 📈 Performance

### Tempos Médios

- **Test**: ~5-10 min
- **Build**: ~15-20 min
- **Deploy**: ~10-15 min
- **Total**: ~30-45 min

### Otimizações

```yaml
# Cache Maven dependencies
- uses: actions/setup-java@v4
  with:
    cache: maven

# Cache Docker layers
cache-from: type=registry,ref=...buildcache
cache-to: type=registry,ref=...buildcache
```

---

## 🔐 Segurança

### GitHub Secrets Required

```bash
✅ DIGITALOCEAN_ACCESS_TOKEN
✅ DB_PASSWORD
✅ KEYCLOAK_CLIENT_SECRET
```

Ver: [.github/GITHUB-SECRETS.md](.github/GITHUB-SECRETS.md)

### Protected Branches

Recomendado no GitHub:

```
Settings > Branches > Branch protection rules

✅ main: Require PR + Reviews
✅ develop: Require PR (opcional)
```

---

## 📚 Workflow File

O workflow completo está em: [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml)

**Principais features**:
- ✅ Parallel testing
- ✅ Multi-stage build
- ✅ Automatic rollback
- ✅ Health checks
- ✅ Vulnerability scanning
- ✅ Environment URLs
- ✅ Deployment notifications

---

## ✅ First Deploy Checklist

```bash
# 1. Configure GitHub Secrets
☐ DIGITALOCEAN_ACCESS_TOKEN
☐ DB_PASSWORD
☐ KEYCLOAK_CLIENT_SECRET

# 2. Verify local build works
☐ make build
☐ make test

# 3. Verify Helm chart
☐ make lint-chart
☐ make template-chart

# 4. Push to trigger CI/CD
☐ git push origin develop

# 5. Monitor deploy
☐ GitHub Actions UI
☐ make logs-dev
☐ make status-dev

# 6. Verify deployment
☐ make health-dev
☐ make port-forward-dev
☐ curl http://localhost:8080/q/health/live

# 7. Done! 🎉
```

---

## 🎉 Success!

Após deploy bem-sucedido, você verá:

```
::notice::=========================================
::notice::✅ Deployment Successful!
::notice::=========================================
::notice::Environment: dev
::notice::Namespace: apporte-workflow-dev
::notice::Image: registry.digitalocean.com/saui/workflow-engine:abc1234
::notice::URL: https://api.apporte.dev/api/workflow
::notice::=========================================
```

Acesse seu app em:
- **Dev**: https://api.apporte.dev/api/workflow
- **Prod**: https://api.apporte.work/api/workflow
