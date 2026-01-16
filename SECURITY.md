# 🔐 Guia de Segurança - Workflow Engine

## 📋 Roles Necessárias no Keycloak

Para o correto funcionamento do sistema, crie as seguintes roles no realm `development`:

### Roles do Workflow Engine

1. **workflow-admin**
   - Acesso completo a todos os endpoints
   - Pode criar, editar, executar e deletar workflows
   - Pode visualizar e gerenciar execuções de qualquer usuário
   - Acesso a métricas e relatórios do sistema

2. **workflow-user**
   - Pode criar e executar workflows próprios
   - Pode visualizar status de execuções próprias
   - Acesso restrito aos próprios workflows

3. **workflow-viewer**
   - Somente leitura
   - Pode visualizar workflows e execuções
   - Acesso restrito aos próprios dados

## 🛡️ Endpoints Protegidos

### Públicos (sem autenticação)
- `GET /q/health/live` - Health check
- `GET /q/health/ready` - Readiness check

### Autenticados (requer token válido)
Todos os endpoints `/api/*` requerem autenticação via Bearer token.

### Por Role

#### Somente Admin (`workflow-admin`, `system-admin`)
- `DELETE /api/workflows/{id}` - Deletar workflow
- `POST /api/workflows/{id}/admin-execute` - Executar workflow como admin
- `GET /api/admin/*` - Todos endpoints administrativos

#### Admin e User (`workflow-admin`, `workflow-user`)
- `POST /api/workflows` - Criar novo workflow
- `PUT /api/workflows/{id}` - Atualizar workflow próprio
- `POST /api/workflows/{id}/execute` - Executar workflow próprio
- `GET /api/workflows` - Listar workflows próprios

#### Admin, User e Viewer (`workflow-admin`, `workflow-user`, `workflow-viewer`)
- `GET /api/workflows/{id}` - Visualizar workflow
- `GET /api/workflows/{id}/executions` - Listar execuções do workflow
- `GET /api/executions/{id}` - Detalhes da execução

## 🔑 Configuração de Segurança

### 1. Criar as roles no Keycloak

Acesse: `https://auth.apporte.work/admin/development/console`

1. Vá em **Realm roles**
2. Clique em **Create role**
3. Crie as 3 roles listadas acima

### 2. Atribuir roles aos usuários

1. Vá em **Users**
2. Selecione o usuário (ex: `admin@example.com`)
3. Aba **Role mapping**
4. Clique em **Assign role**
5. Selecione as roles desejadas

Exemplo de atribuição:
- `admin@example.com` → `workflow-admin`, `system-admin`
- `developer@example.com` → `workflow-user`
- `analyst@example.com` → `workflow-viewer`

### 3. Variáveis de ambiente sensíveis

As seguintes variáveis **NUNCA** devem ser commitadas no git:

```bash
# Database
DB_PASSWORD='...'

# Keycloak
KEYCLOAK_CLIENT_SECRET='...'
```

**Solução implementada:**
- Arquivo `.envrc` está no `.gitignore`
- Arquivo `.envrc.example` criado como template
- Arquivo `.token` (dos testes) está no `.gitignore`

### 4. Como configurar ambiente local

```bash
# 1. Copie o template
cp .envrc.example .envrc

# 2. Edite com valores reais
nano .envrc

# 3. Carregue as variáveis (se usar direnv)
direnv allow

# 4. Ou exporte manualmente
source .envrc
```

## 🧪 Testando a segurança

### 1. Obter token
```bash
# Instale httpie se não tiver
# sudo apt install httpie

# Obter token
http --form POST https://auth.apporte.work/realms/development/protocol/openid-connect/token \
  grant_type=password \
  client_id=workflow-engine-dev \
  client_secret=E6Vy7He2wemRyUDdfXDfyNsOAIwNk43u \
  username=admin@example.com \
  password=admin123 \
  | jq -r '.access_token' > .token
```

### 2. Usar token nos requests
```bash
export TOKEN=$(cat .token)

# Endpoint público (sem token)
curl http://localhost:8080/q/health/live

# Endpoint autenticado
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/workflows

# Endpoint admin-only (requer workflow-admin role)
curl -H "Authorization: Bearer $TOKEN" \
     -X DELETE \
     http://localhost:8080/api/workflows/123
```

### 3. Verificar contexto do usuário
```bash
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/auth/me
```

## 📊 Auditoria

Configure logging para rastrear ações:

```properties
# application-dev.properties
quarkus.log.category."com.apporte.security".level=DEBUG
```

Logs de auditoria incluirão:
- Timestamp da ação
- Usuário (email/ID)
- Recurso acessado
- Operação realizada
- Resultado (sucesso/falha)

## ⚠️ Avisos de Segurança

1. **Proteção de dados pessoais:** Validar que usuários não-admin só acessem seus próprios recursos.

2. **Segregação de responsabilidades:** Cada serviço tem suas próprias roles (`workflow-admin` ≠ `notification-admin`).

3. **Princípio do menor privilégio:** Atribua apenas as roles necessárias para cada usuário.

4. **Rotação de secrets:** Em produção, use secrets manager (AWS Secrets, Azure Key Vault, HashiCorp Vault).

5. **HTTPS obrigatório:** Em produção, configure:
   ```properties
   quarkus.oidc.tls.verification=required
   quarkus.http.ssl.certificate.key-file=path/to/key
   quarkus.http.ssl.certificate.file=path/to/cert
   ```

6. **Rate limiting:** Configure limites de requisições por usuário para prevenir abuso.

7. **Token expiration:** Tokens expiram em 5 minutos (300s). Configure refresh tokens para sessões longas.

## 🔄 SSO (Single Sign-On)

Ambos os serviços (workflow-engine e notification-engine) compartilham:
- Mesmo Keycloak realm (`development`)
- Mesmas roles base (`system-admin`)
- Mesmo usuário pode acessar ambos

Tokens são intercambiáveis entre serviços do mesmo realm.

## 🏗️ Arquitetura de Segurança

```
┌─────────────────────────────────────────────────┐
│           Frontend Application                  │
│      (React/Angular/Vue + TypeScript)           │
└────────────────┬────────────────────────────────┘
                 │
                 │ Bearer Token
                 ▼
┌─────────────────────────────────────────────────┐
│          Keycloak (Auth Server)                 │
│  https://auth.apporte.work/realms/development   │
│                                                 │
│  Roles:                                         │
│  - system-admin                                 │
│  - workflow-admin, workflow-user, workflow-viewer│
│  - notification-admin, notification-sender, ... │
└───────────┬──────────────┬──────────────────────┘
            │              │
            │              │ JWT Token
            │              │
┌───────────▼──────────┐ ┌─▼───────────────────────┐
│  Workflow Engine     │ │  Notification Engine    │
│  Port: 8080          │ │  Port: 8082             │
│  @Authenticated      │ │  @Authenticated         │
│  @RolesAllowed       │ │  @RolesAllowed          │
└──────────────────────┘ └─────────────────────────┘
```
