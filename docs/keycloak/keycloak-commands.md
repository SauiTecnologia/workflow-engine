# Keycloak - Comandos Úteis

## 🔑 Obter Tokens

### Token de Proponente

```bash
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | jq '.'
```

### Token de Admin

```bash
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=admin@apporte.dev' \
  -d 'password=Admin@123' \
  -d 'grant_type=password' | jq '.'
```

### Salvar Token em Variável

```bash
export TOKEN=$(curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | jq -r '.access_token')

echo "Token salvo! Primeiros 50 caracteres: ${TOKEN:0:50}..."
```

---

## 🔍 Decodificar Token

### No Terminal (Linux/Mac)

```bash
# Decodificar payload do JWT
echo "$TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.'
```

### Online

Abrir: https://jwt.io e colar o token

---

## 🧪 Testar Endpoints

### Endpoint Público

```bash
curl http://localhost:8081/api/auth/public | jq '.'
```

### Endpoint Protegido (sem token - deve retornar 401)

```bash
curl -v http://localhost:8081/api/auth/me
```

### Endpoint Protegido (com token)

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/me | jq '.'
```

### Endpoint de Proponente

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/proponente | jq '.'
```

### Endpoint de Admin (deve retornar 403 se não for admin)

```bash
curl -v -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/admin
```

### Endpoint de Debug

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/debug | jq '.'
```

### Endpoint de Verificação de Organização

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/org-check | jq '.'
```

---

## 🔄 Refresh Token

```bash
# Obter tokens (access + refresh)
RESPONSE=$(curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password')

export ACCESS_TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')
export REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refresh_token')

# Usar refresh token para obter novo access token
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d "refresh_token=$REFRESH_TOKEN" \
  -d 'grant_type=refresh_token' | jq '.'
```

---

## 🔐 Validar Token

```bash
# Introspect token (verificar se é válido)
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token/introspect' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d "token=$TOKEN" | jq '.'
```

---

## 👤 Obter Informações do Usuário

```bash
# UserInfo endpoint
curl -H "Authorization: Bearer $TOKEN" \
  https://auth.apporte.work/realms/development/protocol/openid-connect/userinfo | jq '.'
```

---

## 🚪 Logout

```bash
# Logout (invalidar token)
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/logout' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d "refresh_token=$REFRESH_TOKEN"
```

---

## 📊 Verificar Configuração OIDC

```bash
# Well-known configuration
curl -s https://auth.apporte.work/realms/development/.well-known/openid-configuration | jq '.'

# Apenas endpoints importantes
curl -s https://auth.apporte.work/realms/development/.well-known/openid-configuration | jq '{
  issuer,
  authorization_endpoint,
  token_endpoint,
  userinfo_endpoint,
  jwks_uri,
  grant_types_supported,
  response_types_supported
}'
```

---

## 🔑 JWKS (Chaves Públicas)

```bash
# Obter chaves públicas para validar JWT
curl -s https://auth.apporte.work/realms/development/protocol/openid-connect/certs | jq '.'
```

---

## 🏥 Health Checks

### Keycloak

```bash
# Health check do Keycloak
curl -s https://auth.apporte.work/health | jq '.'

# Readiness
curl -s https://auth.apporte.work/health/ready | jq '.'

# Liveness
curl -s https://auth.apporte.work/health/live | jq '.'
```

### Backend

```bash
# Health check do backend
curl -s http://localhost:8081/q/health | jq '.'

# Readiness
curl -s http://localhost:8081/q/health/ready | jq '.'

# Liveness
curl -s http://localhost:8081/q/health/live | jq '.'
```

---

## 📝 Logs

### Backend (Local)

```bash
# Ver logs em tempo real
tail -f /home/joaopedro/workflow-engine/target/quarkus.log

# Filtrar apenas logs de autenticação
tail -f /home/joaopedro/workflow-engine/target/quarkus.log | grep -i "oidc\|auth\|token\|jwt"

# Ver últimas 100 linhas
tail -100 /home/joaopedro/workflow-engine/target/quarkus.log
```

### Keycloak (Kubernetes)

```bash
# Ver logs do Keycloak
kubectl logs -n keycloak-shared deployment/keycloak -f

# Filtrar eventos de login
kubectl logs -n keycloak-shared deployment/keycloak | grep "LOGIN\|LOGOUT"

# Ver logs dos últimos 10 minutos
kubectl logs -n keycloak-shared deployment/keycloak --since=10m
```

---

## 🧪 Script de Testes Completo

```bash
# Executar todos os testes
cd /home/joaopedro/workflow-engine
./scripts/test-keycloak-integration.sh

# Ver apenas falhas
./scripts/test-keycloak-integration.sh 2>&1 | grep -E "✗|FALHOU|ERROR"
```

---

## 🔧 Desenvolvimento

### Iniciar Backend com Keycloak

```bash
cd /home/joaopedro/workflow-engine

# Com profile dev-keycloak
./mvnw quarkus:dev -Dquarkus.profile=dev-keycloak

# Com debug remoto habilitado
./mvnw quarkus:dev -Dquarkus.profile=dev-keycloak -Ddebug=5005
```

### Recarregar Configuração (Hot Reload)

Quarkus recarrega automaticamente. Apenas salve o arquivo:
- `application-dev-keycloak.properties`
- Classes Java

---

## 🐳 Docker (se usar localmente)

### Keycloak Local (para desenvolvimento sem cluster)

```bash
# Iniciar Keycloak local
docker run -d \
  --name keycloak-local \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:23.0.0 \
  start-dev

# Acessar: http://localhost:8080
```

---

## 📊 Métricas

```bash
# Métricas do Backend (Prometheus format)
curl http://localhost:8081/q/metrics

# Filtrar métricas de HTTP
curl http://localhost:8081/q/metrics | grep http_

# Métricas de autenticação
curl http://localhost:8081/q/metrics | grep -i auth
```

---

## 🛠️ Troubleshooting

### Verificar conectividade Keycloak → Backend

```bash
# Do backend, tentar acessar Keycloak
curl -v https://auth.apporte.work/realms/development/.well-known/openid-configuration
```

### Verificar Client Secret

```bash
# Ver configuração (secret está mascarado no log)
grep "client-id\|credentials" /home/joaopedro/workflow-engine/src/main/resources/application-dev-keycloak.properties
```

### Verificar CORS

```bash
# Fazer requisição OPTIONS (preflight)
curl -X OPTIONS http://localhost:8081/api/auth/me \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v
```

### Testar Token Expirado

```bash
# Usar um token inválido
curl -v -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token" \
  http://localhost:8081/api/auth/me
```

---

## 🎯 One-Liners Úteis

### Obter e testar token em um comando

```bash
curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | \
  jq -r '.access_token' | \
  xargs -I {} curl -H "Authorization: Bearer {}" http://localhost:8081/api/auth/me | jq '.'
```

### Ver claims do token direto

```bash
curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | \
  jq -r '.access_token' | \
  cut -d'.' -f2 | \
  base64 -d 2>/dev/null | \
  jq '.'
```

### Testar todos os endpoints como proponente

```bash
TOKEN=$(curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | jq -r '.access_token')

echo "=== /me ===" && \
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/me | jq '.' && \
echo -e "\n=== /proponente ===" && \
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/proponente | jq '.' && \
echo -e "\n=== /admin (deve falhar) ===" && \
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/admin | jq '.'
```

---

## 📚 Links Rápidos

- **Keycloak Admin**: https://auth.apporte.work/admin
- **OIDC Config**: https://auth.apporte.work/realms/development/.well-known/openid-configuration
- **JWT Decoder**: https://jwt.io
- **Backend Health**: http://localhost:8081/q/health
- **Backend Dev UI**: http://localhost:8081/q/dev

---

## 💾 Exportar/Importar Configuração Keycloak

### Exportar Realm (via Admin Console)

1. Keycloak Admin → Realm Settings → Action → Partial export
2. Marcar: Include Groups and roles, Include Clients, Include Identity providers
3. Download JSON

### Importar Realm (via kubectl)

```bash
# Criar ConfigMap com configuração
kubectl create configmap keycloak-realm-import \
  --from-file=realm.json \
  -n keycloak-shared

# Reiniciar Keycloak para importar
kubectl rollout restart deployment/keycloak -n keycloak-shared
```

---

**Dica**: Salve este arquivo como referência rápida! 📌
