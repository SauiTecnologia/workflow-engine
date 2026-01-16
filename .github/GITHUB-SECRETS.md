# GitHub Secrets Setup

Secrets necessários para o CI/CD do Workflow Engine.

## 🔐 Required Secrets

Configure estes secrets no GitHub: `Settings > Secrets and variables > Actions > New repository secret`

### Infrastructure

#### `DIGITALOCEAN_ACCESS_TOKEN`
- **Descrição**: Token de acesso do DigitalOcean para kubectl e registry
- **Como obter**:
  ```bash
  # No painel DigitalOcean:
  # API > Tokens > Generate New Token
  # Scopes: read + write
  ```
- **Usado em**: Autenticação doctl, kubectl, registry push

### Application Secrets

#### `DB_PASSWORD`
- **Descrição**: Senha do banco de dados PostgreSQL (Supabase)
- **Valor atual**: (da sua .envrc)
- **Usado em**: Deployment via Helm (`--set secrets.database.password`)

#### `KEYCLOAK_CLIENT_SECRET`
- **Descrição**: Client secret do Keycloak para autenticação OIDC
- **Como obter**:
  ```bash
  # Keycloak Admin Console:
  # Clients > workflow-engine-dev/prod > Credentials > Client Secret
  ```
- **Usado em**: Deployment via Helm (`--set secrets.keycloak.clientSecret`)

---

## 📋 Setup Checklist

```bash
# 1. Acesse o repositório no GitHub
https://github.com/YOUR_ORG/workflow-engine

# 2. Vá em Settings > Secrets and variables > Actions

# 3. Adicione cada secret:
✅ DIGITALOCEAN_ACCESS_TOKEN
✅ DB_PASSWORD
✅ KEYCLOAK_CLIENT_SECRET

# 4. Verifique se estão salvos
```

---

## 🧪 Testar Secrets Localmente

Antes de commitar, teste se os secrets estão corretos:

```bash
# 1. Verificar .envrc
cat .envrc | grep -E "(DB_PASSWORD|OIDC_CLIENT_SECRET)"

# 2. Testar conexão com banco
psql "postgresql://workflow_backend:$DB_PASSWORD@db.rpkqbesfgjdeolketoug.supabase.co:5432/postgres?sslmode=require"

# 3. Testar Keycloak
./test-keycloak.sh
```

---

## 🔄 Como o CI/CD Usa os Secrets

### Build Phase
```yaml
# Não usa secrets - apenas compila
- name: Build with Maven
  run: ./mvnw clean package -DskipTests
```

### Deploy Phase
```yaml
# Injeta secrets no Helm
helm upgrade --install workflow-engine ./charts \
  --set secrets.database.password="${{ secrets.DB_PASSWORD }}" \
  --set secrets.keycloak.clientSecret="${{ secrets.KEYCLOAK_CLIENT_SECRET }}"
```

### No Kubernetes
Os secrets são criados como `Secret` resource:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: workflow-engine-secrets
type: Opaque
stringData:
  DB_PASSWORD: <valor-do-github-secret>
  KEYCLOAK_CLIENT_SECRET: <valor-do-github-secret>
```

E injetados no pod via `envFrom`:
```yaml
envFrom:
- secretRef:
    name: workflow-engine-secrets
```

---

## 🔐 Security Best Practices

### ✅ DO
- ✅ Use GitHub Secrets (nunca hardcode)
- ✅ Rotate secrets periodicamente
- ✅ Use diferentes secrets para dev/prod
- ✅ Limite acesso aos secrets (protected branches)
- ✅ Audit logs de uso de secrets

### ❌ DON'T
- ❌ Commitar secrets em código
- ❌ Logar secrets em CI/CD
- ❌ Compartilhar secrets por email/chat
- ❌ Usar mesmos secrets em dev e prod
- ❌ Armazenar secrets em plain text localmente

---

## 🌍 Ambientes

### Development
- Namespace: `apporte-workflow-dev`
- URL: `https://api.apporte.dev/api/workflow`
- Keycloak Client: `workflow-engine-dev`
- Secrets: Mesmos secrets do GitHub (compartilhados dev/prod por enquanto)

### Production
- Namespace: `apporte-workflow-prod`
- URL: `https://api.apporte.work/api/workflow`
- Keycloak Client: `workflow-engine-prod`
- Secrets: Mesmos secrets do GitHub (compartilhados dev/prod por enquanto)

**⚠️ Recomendação**: No futuro, criar secrets separados:
- `DB_PASSWORD_DEV` / `DB_PASSWORD_PROD`
- `KEYCLOAK_CLIENT_SECRET_DEV` / `KEYCLOAK_CLIENT_SECRET_PROD`

---

## 🐛 Troubleshooting

### Secret não encontrado no CI/CD
```bash
# Erro: "secrets.DB_PASSWORD" is not defined
# Solução: Adicione o secret no GitHub (Settings > Secrets)
```

### Secret com valor incorreto
```bash
# Erro: Health check failed / Database connection refused
# Solução:
1. Verifique o valor do secret no GitHub
2. Re-teste localmente com .envrc
3. Update o secret no GitHub
4. Re-run o workflow
```

### Como ver secrets no cluster (debug)
```bash
# Ver secret (base64 encoded)
kubectl get secret workflow-engine-secrets -n apporte-workflow-dev -o yaml

# Decodificar secret
kubectl get secret workflow-engine-secrets -n apporte-workflow-dev \
  -o jsonpath='{.data.DB_PASSWORD}' | base64 -d

# Ver todas as env vars no pod
kubectl exec -n apporte-workflow-dev deployment/workflow-engine -- env | grep -E "(DB_|KEYCLOAK_)"
```

---

## 📚 Referências

- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [Helm Secrets Management](https://helm.sh/docs/chart_best_practices/secrets/)

---

## ✅ Quick Setup

```bash
# 1. Get values from .envrc
source .envrc
echo "DB_PASSWORD: $DB_PASSWORD"
echo "KEYCLOAK_CLIENT_SECRET: $OIDC_CLIENT_SECRET"

# 2. Add to GitHub:
# https://github.com/YOUR_ORG/workflow-engine/settings/secrets/actions/new

# 3. Test workflow:
git push origin develop

# 4. Monitor:
# https://github.com/YOUR_ORG/workflow-engine/actions
```
