# Guia de Configuração do Keycloak - Apporte 2.0

## 🏗️ Arquitetura Multi-Tenant

O Apporte 2.0 utiliza uma arquitetura **multi-tenant** baseada em organizações:

```
Sistema Apporte
├── Administradores do Sistema (gerenciam tudo)
└── Organizações (multi-tenant)
    ├── Proponentes (criam projetos/propostas)
    ├── Pareceristas (avaliam propostas)
    └── Investidores (investem em projetos)
```

## 📍 Informações do Keycloak

- **URL Pública**: https://auth.apporte.work
- **Admin Console**: https://auth.apporte.work/admin
- **Namespace K8s**: `keycloak-shared`

---

## 🎯 Passo 1: Configurar Realms

### 1.1 Realm Development

1. Acesse: https://auth.apporte.work/admin
2. Clique no dropdown "master" (canto superior esquerdo)
3. Clique em **"Create Realm"**
4. Configure:

```yaml
Realm name: development
Display name: Apporte Development
Enabled: ON
```

5. Clique em **"Create"**

#### Configurações do Realm Development

Na aba **"Realm Settings"**:

**General**:
- User registration: `Enabled` ✅
- Forgot password: `Enabled` ✅
- Remember me: `Enabled` ✅

**Login**:
- Email as username: `Enabled` ✅
- Login with email: `Enabled` ✅
- Verify email: `Disabled` ⛔ (facilitar testes)

**Tokens**:
- Access Token Lifespan: `30 minutes`
- SSO Session Idle: `30 minutes`
- SSO Session Max: `10 hours`

**Security Defenses**:
- Brute Force Detection: `Disabled` (dev environment)

---

### 1.2 Realm Production

Repita o processo para criar o realm de produção:

```yaml
Realm name: production
Display name: Apporte Production
Enabled: ON
```

#### Configurações do Realm Production

**General**:
- User registration: `Disabled` ⛔ (apenas convites)
- Forgot password: `Enabled` ✅
- Remember me: `Enabled` ✅

**Login**:
- Email as username: `Enabled` ✅
- Login with email: `Enabled` ✅
- Verify email: `Enabled` ✅ (obrigatório)

**Tokens**:
- Access Token Lifespan: `15 minutes` (mais seguro)
- SSO Session Idle: `30 minutes`
- SSO Session Max: `8 hours`

**Security Defenses**:
- Brute Force Detection: `Enabled` ✅
  - Max Login Failures: `5`
  - Wait Increment: `60 seconds`
  - Max Wait: `15 minutes`

---

## 🎭 Passo 2: Criar Roles (Realm Roles)

> **IMPORTANTE**: Faça isso em **AMBOS** os realms (development e production)

### 2.1 Roles de Sistema

1. No realm selecionado, vá em **"Realm roles"** (menu lateral)
2. Clique em **"Create role"**
3. Crie as seguintes roles:

#### Role: `system-admin`
```yaml
Role name: system-admin
Description: Administrador do sistema Apporte - acesso total
```

---

### 2.2 Roles de Organização

Crie as seguintes roles para usuários dentro de organizações:

#### Role: `org-admin`
```yaml
Role name: org-admin
Description: Administrador da organização - gerencia membros e configurações
```

#### Role: `proponente`
```yaml
Role name: proponente
Description: Cria e gerencia propostas/projetos dentro da organização
```

#### Role: `parecerista`
```yaml
Role name: parecerista
Description: Avalia e emite pareceres sobre propostas
```

#### Role: `investidor`
```yaml
Role name: investidor
Description: Visualiza projetos e realiza investimentos
```

#### Role: `viewer`
```yaml
Role name: viewer
Description: Visualização básica - apenas leitura
```

---

### 2.3 Composite Roles (Hierarquia)

Agora vamos configurar hierarquia de permissões:

#### Configurar `system-admin`:
1. Clique na role `system-admin`
2. Vá na aba **"Associated roles"**
3. Clique em **"Assign role"**
4. Selecione TODAS as outras roles criadas
5. Clique em **"Assign"**

Isso faz com que `system-admin` herde todas as permissões.

#### Configurar `org-admin`:
1. Clique na role `org-admin`
2. Vá na aba **"Associated roles"**
3. Selecione: `proponente`, `parecerista`, `investidor`, `viewer`
4. Clique em **"Assign"**

---

## 👥 Passo 3: Configurar Grupos (Organizações)

Os grupos representam **organizações** no sistema.

### 3.1 Criar Grupo de Exemplo (Development)

1. Vá em **"Groups"** (menu lateral)
2. Clique em **"Create group"**
3. Configure:

```yaml
Name: org-exemplo
```

4. Clique em **"Create"**

### 3.2 Adicionar Roles ao Grupo

1. Clique no grupo `org-exemplo`
2. Vá na aba **"Role mapping"**
3. Clique em **"Assign role"**
4. Selecione as roles que os membros deste grupo podem ter:
   - `proponente`
   - `parecerista`
   - `investidor`
   - `viewer`
5. Clique em **"Assign"**

> **NOTA**: Cada organização real será um grupo separado. Os usuários serão adicionados aos grupos e receberão roles específicas dentro daquele grupo.

---

## 🔌 Passo 4: Criar Clients (Frontend e Backend)

### 4.1 Client: Frontend Development

1. Vá em **"Clients"** (menu lateral) no realm `development`
2. Clique em **"Create client"**

**General Settings**:
```yaml
Client type: OpenID Connect
Client ID: apporte-frontend-dev
Name: Apporte Frontend (Development)
Description: React SPA para desenvolvimento
Always display in console: OFF
```

Clique em **"Next"**

**Capability config**:
```yaml
Client authentication: OFF (público)
Authorization: OFF
Authentication flow:
  ✅ Standard flow (código de autorização)
  ✅ Direct access grants (senha - apenas dev)
  ⛔ Implicit flow
  ⛔ Service accounts roles
```

Clique em **"Next"**

**Login settings**:
```yaml
Root URL: http://localhost:3000
Home URL: http://localhost:3000
Valid redirect URIs: 
  - http://localhost:3000/*
  - http://localhost:5173/*
  - https://*.replit.dev/*
Valid post logout redirect URIs: +
Web origins:
  - http://localhost:3000
  - http://localhost:5173
  - https://*.replit.dev
```

Clique em **"Save"**

#### Configurações Adicionais do Client

Na aba **"Advanced"**:
```yaml
Access Token Lifespan: (deixar padrão do realm)
```

---

### 4.2 Client: Frontend Production

Repita o processo no realm `production`:

**General Settings**:
```yaml
Client ID: apporte-frontend-prod
Name: Apporte Frontend (Production)
```

**Capability config**: (mesmo que dev, mas sem "Direct access grants")
```yaml
Client authentication: OFF
Authorization: OFF
Authentication flow:
  ✅ Standard flow
  ⛔ Direct access grants (senha - não usar em prod)
  ⛔ Implicit flow
  ⛔ Service accounts roles
```

**Login settings**:
```yaml
Root URL: https://app.apporte.work
Home URL: https://app.apporte.work
Valid redirect URIs: 
  - https://app.apporte.work/*
  - https://*.apporte.work/*
Valid post logout redirect URIs: +
Web origins:
  - https://app.apporte.work
  - https://*.apporte.work
```

---

### 4.3 Client: Backend Development

1. No realm `development`, crie novo client:

**General Settings**:
```yaml
Client ID: workflow-engine-dev
Name: Workflow Engine API (Development)
Description: Backend Quarkus API - Bearer only
```

**Capability config**:
```yaml
Client authentication: ON (confidencial)
Authorization: OFF
Authentication flow:
  ⛔ Standard flow
  ⛔ Direct access grants
  ⛔ Implicit flow
  ✅ Service accounts roles (para M2M se necessário)
```

**Login settings**: (deixar vazio - é bearer-only)

Clique em **"Save"**

#### Obter Secret do Client

1. Na aba **"Credentials"**
2. Copie o **"Client secret"** - você vai precisar dele!

```bash
# Exemplo (o seu será diferente):
Client Secret: a8f3k2j9-1234-5678-90ab-cdef12345678
```

---

### 4.4 Client: Backend Production

Repita para realm `production`:

```yaml
Client ID: workflow-engine-prod
Name: Workflow Engine API (Production)
Client authentication: ON
Service accounts roles: ON
```

Copie o **Client Secret** de produção também.

---

## 🎫 Passo 5: Configurar Client Scopes (Adicionar org_id ao Token)

Para suportar multi-tenancy, precisamos adicionar o ID da organização ao token JWT.

### 5.1 Criar Client Scope

1. Vá em **"Client scopes"** (menu lateral)
2. Clique em **"Create client scope"**

```yaml
Name: organization
Description: Adiciona informações da organização ao token
Type: Default
Protocol: OpenID Connect
Display on consent screen: OFF
Include in token scope: ON
```

3. Clique em **"Save"**

### 5.2 Adicionar Mapper para org_id

1. Clique no scope `organization` que acabou de criar
2. Vá na aba **"Mappers"**
3. Clique em **"Add mapper"** → **"By configuration"**
4. Selecione **"User Attribute"**

Configure o mapper:
```yaml
Name: organization-id
User Attribute: organization_id
Token Claim Name: org_id
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
Multivalued: OFF
```

5. Clique em **"Save"**

### 5.3 Adicionar Mapper para org_name

Repita o processo:

```yaml
Name: organization-name
User Attribute: organization_name
Token Claim Name: org_name
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

### 5.4 Adicionar Mapper para Groups

1. Clique em **"Add mapper"** → **"By configuration"**
2. Selecione **"Group Membership"**

```yaml
Name: groups
Token Claim Name: groups
Full group path: OFF (apenas nome do grupo)
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

### 5.5 Associar Scope aos Clients

1. Vá em **"Clients"** → selecione `apporte-frontend-dev`
2. Vá na aba **"Client scopes"**
3. Clique em **"Add client scope"**
4. Selecione `organization`
5. Selecione tipo: **"Default"**
6. Clique em **"Add"**

Repita para TODOS os clients (frontend-dev, frontend-prod, workflow-engine-dev, workflow-engine-prod).

---

## 👤 Passo 6: Criar Usuários de Teste (Development)

### 6.1 Usuário Admin do Sistema

1. Vá em **"Users"** no realm `development`
2. Clique em **"Add user"**

```yaml
Username: admin@apporte.dev
Email: admin@apporte.dev
Email verified: ON
First name: Admin
Last name: Sistema
```

3. Clique em **"Create"**

#### Configurar Senha:
1. Vá na aba **"Credentials"**
2. Clique em **"Set password"**
3. Digite: `Admin@123` (ou outra senha forte)
4. **Temporary**: `OFF` ⛔
5. Clique em **"Save"**

#### Adicionar Role:
1. Vá na aba **"Role mapping"**
2. Clique em **"Assign role"**
3. Selecione `system-admin`
4. Clique em **"Assign"**

---

### 6.2 Usuário Proponente de Organização

1. Crie novo usuário:

```yaml
Username: proponente@exemplo.com
Email: proponente@exemplo.com
Email verified: ON
First name: João
Last name: Proponente
```

#### Configurar Senha:
- Senha: `Proponente@123`
- Temporary: OFF

#### Adicionar ao Grupo:
1. Vá na aba **"Groups"**
2. Clique em **"Join Group"**
3. Selecione `org-exemplo`
4. Clique em **"Join"**

#### Adicionar Role:
1. Vá na aba **"Role mapping"**
2. Clique em **"Assign role"**
3. Selecione `proponente`
4. Clique em **"Assign"**

#### Adicionar Atributos de Organização:
1. Vá na aba **"Attributes"**
2. Clique em **"Add an attribute"**
3. Adicione:

```yaml
Key: organization_id
Value: org-exemplo-001

Key: organization_name
Value: Organização Exemplo
```

4. Clique em **"Save"**

---

### 6.3 Usuário Parecerista

Repita o processo:

```yaml
Username: parecerista@exemplo.com
Email: parecerista@exemplo.com
First name: Maria
Last name: Parecerista
Password: Parecerista@123
Group: org-exemplo
Role: parecerista
Attributes:
  organization_id: org-exemplo-001
  organization_name: Organização Exemplo
```

---

### 6.4 Usuário Investidor

```yaml
Username: investidor@exemplo.com
Email: investidor@exemplo.com
First name: Carlos
Last name: Investidor
Password: Investidor@123
Group: org-exemplo
Role: investidor
Attributes:
  organization_id: org-exemplo-001
  organization_name: Organização Exemplo
```

---

## ✅ Passo 7: Testar Configuração

### 7.1 Testar Login Direto (Development)

Use este comando para obter um token:

```bash
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password'
```

**Resultado esperado**: JSON com `access_token`, `refresh_token`, etc.

### 7.2 Decodificar Token

Copie o `access_token` e decodifique em: https://jwt.io

Você deve ver:
```json
{
  "sub": "...",
  "email": "proponente@exemplo.com",
  "name": "João Proponente",
  "realm_access": {
    "roles": ["proponente"]
  },
  "org_id": "org-exemplo-001",
  "org_name": "Organização Exemplo",
  "groups": ["org-exemplo"]
}
```

---

## 📝 Resumo das Credenciais

### Development Realm

| Usuário | Senha | Role | Organização |
|---------|-------|------|-------------|
| admin@apporte.dev | Admin@123 | system-admin | - |
| proponente@exemplo.com | Proponente@123 | proponente | org-exemplo |
| parecerista@exemplo.com | Parecerista@123 | parecerista | org-exemplo |
| investidor@exemplo.com | Investidor@123 | investidor | org-exemplo |

### Clients

| Client ID | Secret | Realm | Uso |
|-----------|--------|-------|-----|
| apporte-frontend-dev | (público) | development | React SPA |
| apporte-frontend-prod | (público) | production | React SPA |
| workflow-engine-dev | `[copie do Keycloak]` | development | Backend API |
| workflow-engine-prod | `[copie do Keycloak]` | production | Backend API |

---

## 🔄 Próximos Passos

Após concluir esta configuração:

1. ✅ Integrar backend (workflow-engine) com Keycloak OIDC
2. ✅ Integrar frontend React com keycloak-js
3. ✅ Testar fluxo completo de autenticação
4. ✅ Implementar controle de acesso baseado em roles no backend

---

## 🆘 Troubleshooting

### Token não contém org_id

**Solução**: Verifique se:
1. O client scope `organization` está associado ao client
2. Os mappers estão configurados corretamente
3. O usuário tem os atributos `organization_id` e `organization_name`

### Erro "Invalid redirect_uri"

**Solução**: Adicione a URI exata em "Valid redirect URIs" do client

### Usuário não consegue logar

**Solução**: Verifique:
1. Email verified está ON (ou desative verificação no realm)
2. Usuário está ativo (Enabled = ON)
3. Senha foi configurada corretamente (Temporary = OFF)

---

**🎉 Configuração do Keycloak concluída!**
