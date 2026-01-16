# Guia de Testes - Integração Keycloak Apporte 2.0

## 🎯 Objetivo

Validar a integração completa entre Keycloak, Backend (workflow-engine) e Frontend (React).

---

## ✅ Pré-requisitos

Antes de iniciar os testes, certifique-se de que:

1. ✅ Keycloak está acessível em https://auth.apporte.work
2. ✅ Configuração manual do Keycloak foi concluída (realms, clients, users)
3. ✅ Backend está rodando (local ou deployed)
4. ✅ Frontend está rodando (local ou Replit)

---

## 🧪 Fase 1: Testes Manuais do Keycloak

### 1.1 Verificar Configuração OIDC

```bash
# Verificar se realm está acessível
curl -s https://auth.apporte.work/realms/development/.well-known/openid-configuration | jq '.'
```

**Resultado esperado**: JSON com configurações OIDC (issuer, token_endpoint, etc.)

### 1.2 Testar Login com curl

```bash
# Obter token de um usuário
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | jq '.'
```

**Resultado esperado**: JSON com `access_token`, `refresh_token`, `expires_in`, etc.

### 1.3 Decodificar Token JWT

Copie o `access_token` do passo anterior e cole em: https://jwt.io

**Verifique**:
- ✅ `email`: proponente@exemplo.com
- ✅ `name`: João Proponente
- ✅ `org_id`: org-exemplo-001
- ✅ `org_name`: Organização Exemplo
- ✅ `realm_access.roles`: ["proponente"]
- ✅ `groups`: ["org-exemplo"]

---

## 🚀 Fase 2: Testes do Backend

### 2.1 Iniciar Backend com Profile Keycloak

```bash
cd /home/joaopedro/workflow-engine

# Obter client secret do Keycloak
# (Keycloak Admin → Clients → workflow-engine-dev → Credentials)

# Editar application-dev-keycloak.properties
nano src/main/resources/application-dev-keycloak.properties
# Substituir: quarkus.oidc.credentials.secret=SEU_SECRET_AQUI

# Iniciar com profile dev-keycloak
./mvnw quarkus:dev -Dquarkus.profile=dev-keycloak
```

**Aguardar**: `Listening on: http://localhost:8081`

### 2.2 Testar Endpoints Públicos

```bash
# Health check (deve funcionar sem token)
curl http://localhost:8081/q/health | jq '.'

# Endpoint público
curl http://localhost:8081/api/auth/public | jq '.'
```

**Resultado esperado**: HTTP 200 OK

### 2.3 Testar Endpoint Protegido (sem token)

```bash
# Deve retornar 401 Unauthorized
curl -v http://localhost:8081/api/auth/me
```

**Resultado esperado**: HTTP 401 Unauthorized

### 2.4 Testar Endpoint Protegido (com token)

```bash
# 1. Obter token
export TOKEN=$(curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | jq -r '.access_token')

echo "Token obtido: ${TOKEN:0:50}..."

# 2. Chamar endpoint protegido
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/me | jq '.'
```

**Resultado esperado**:
```json
{
  "userId": "...",
  "email": "proponente@exemplo.com",
  "name": "João Proponente",
  "username": "proponente@exemplo.com",
  "organizationId": "org-exemplo-001",
  "organizationName": "Organização Exemplo",
  "groups": ["org-exemplo"],
  "roles": ["proponente"],
  "isSystemAdmin": false,
  "isOrgAdmin": false,
  "isProponente": true,
  "isParecerista": false,
  "isInvestidor": false
}
```

### 2.5 Testar Controle de Acesso por Role

```bash
# Proponente PODE acessar endpoint de proponente
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/proponente | jq '.'
# Esperado: HTTP 200 OK

# Proponente NÃO PODE acessar endpoint de admin
curl -v -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/admin
# Esperado: HTTP 403 Forbidden
```

### 2.6 Testar como Admin

```bash
# Obter token de admin
export ADMIN_TOKEN=$(curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=admin@apporte.dev' \
  -d 'password=Admin@123' \
  -d 'grant_type=password' | jq -r '.access_token')

# Admin PODE acessar endpoint de admin
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8081/api/auth/admin | jq '.'
# Esperado: HTTP 200 OK

# Admin PODE acessar endpoint de proponente (herda roles)
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8081/api/auth/proponente | jq '.'
# Esperado: HTTP 200 OK
```

---

## 🌐 Fase 3: Testes do Frontend

### 3.1 Iniciar Frontend (Local)

```bash
cd /path/to/frontend

# Criar .env
cat > .env << EOF
REACT_APP_KEYCLOAK_REALM=development
REACT_APP_KEYCLOAK_CLIENT_ID=apporte-frontend-dev
REACT_APP_API_URL=http://localhost:8081
EOF

# Instalar dependências
npm install keycloak-js

# Iniciar
npm start
```

### 3.2 Teste Manual no Browser

1. **Abrir**: http://localhost:3000
2. **Verificar**: Botão "Fazer Login" aparece
3. **Clicar** em "Fazer Login"
4. **Redirecionar** para: https://auth.apporte.work/realms/development/...
5. **Fazer login** com:
   - Email: `proponente@exemplo.com`
   - Senha: `Proponente@123`
6. **Redirecionar** de volta para: http://localhost:3000
7. **Verificar**: Nome do usuário aparece ("Olá, João Proponente!")
8. **Abrir** DevTools → Console → verificar logs do Keycloak
9. **Verificar** DevTools → Application → Local Storage → verificar token

### 3.3 Teste de Chamada à API

No DevTools Console:

```javascript
// Verificar se token foi salvo
console.log('Token:', window.keycloak?.token);

// Fazer chamada à API
fetch('http://localhost:8081/api/auth/me', {
  headers: {
    'Authorization': `Bearer ${window.keycloak.token}`
  }
})
.then(res => res.json())
.then(data => console.log('User info:', data));
```

### 3.4 Teste de Proteção de Rotas

1. **Acessar** rota protegida (ex: `/propostas`)
2. **Verificar**: Conteúdo aparece apenas se tiver role `proponente`
3. **Fazer logout**
4. **Tentar acessar** `/propostas` diretamente
5. **Verificar**: Redireciona para login ou mostra "Acesso Negado"

---

## 🤖 Fase 4: Testes Automatizados

### 4.1 Usar Script de Teste

```bash
cd /home/joaopedro/workflow-engine

# Executar script de testes
./scripts/test-keycloak-integration.sh
```

**O script testa**:
- ✅ Conectividade com Keycloak
- ✅ Conectividade com Backend
- ✅ Obtenção de tokens
- ✅ Decodificação de tokens
- ✅ Endpoints públicos
- ✅ Endpoints protegidos sem token (401)
- ✅ Endpoints protegidos com token (200)
- ✅ Controle de acesso por role (403 quando não tem permissão)
- ✅ Admin tem acesso a tudo

### 4.2 Testar com Postman/Insomnia

**Importar Collection**:

Criar arquivo `keycloak-tests.json`:

```json
{
  "info": {
    "name": "Apporte Keycloak Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Get Token (Proponente)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/x-www-form-urlencoded"
          }
        ],
        "body": {
          "mode": "urlencoded",
          "urlencoded": [
            {"key": "client_id", "value": "apporte-frontend-dev"},
            {"key": "username", "value": "proponente@exemplo.com"},
            {"key": "password", "value": "Proponente@123"},
            {"key": "grant_type", "value": "password"}
          ]
        },
        "url": {
          "raw": "https://auth.apporte.work/realms/development/protocol/openid-connect/token",
          "protocol": "https",
          "host": ["auth", "apporte", "work"],
          "path": ["realms", "development", "protocol", "openid-connect", "token"]
        }
      }
    },
    {
      "name": "2. Get User Info",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{access_token}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8081/api/auth/me",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8081",
          "path": ["api", "auth", "me"]
        }
      }
    }
  ]
}
```

Importar no Postman e executar os requests.

---

## 📊 Fase 5: Monitoramento e Logs

### 5.1 Logs do Backend

```bash
# Ver logs em tempo real
tail -f /home/joaopedro/workflow-engine/target/quarkus.log

# Filtrar logs de autenticação
tail -f /home/joaopedro/workflow-engine/target/quarkus.log | grep -i "oidc\|auth\|token"
```

### 5.2 Logs do Keycloak (Kubernetes)

```bash
# Ver logs do Keycloak
kubectl logs -n keycloak-shared deployment/keycloak -f

# Ver eventos de autenticação
kubectl logs -n keycloak-shared deployment/keycloak | grep "LOGIN\|LOGOUT"
```

### 5.3 Métricas do Backend

```bash
# Health check
curl http://localhost:8081/q/health | jq '.'

# Métricas Prometheus
curl http://localhost:8081/q/metrics
```

---

## 🐛 Troubleshooting

### Problema: Backend retorna 401 mesmo com token válido

**Diagnóstico**:
```bash
# Verificar se backend consegue acessar Keycloak
curl -v https://auth.apporte.work/realms/development/.well-known/openid-configuration
```

**Soluções**:
1. Verificar client secret está correto
2. Verificar realm está correto (development vs production)
3. Ver logs do backend: `tail -f target/quarkus.log`

### Problema: Token não contém org_id

**Diagnóstico**:
Decodificar token em jwt.io e verificar claims.

**Soluções**:
1. Verificar se client scope `organization` está associado ao client
2. Verificar se usuário tem atributos `organization_id` e `organization_name`
3. Verificar mappers do client scope

### Problema: Frontend não redireciona após login

**Diagnóstico**:
Abrir DevTools → Console → verificar erros.

**Soluções**:
1. Verificar "Valid redirect URIs" no Keycloak
2. Adicionar URI exata do Replit: `https://seu-projeto.replit.dev/*`
3. Verificar configuração CORS no backend

### Problema: CORS errors

**Solução**:
Adicionar origem do frontend no `application-dev-keycloak.properties`:

```properties
quarkus.http.cors.origins=http://localhost:3000,https://seu-projeto.replit.dev
```

---

## ✅ Checklist Final

### Keycloak
- [ ] Realm `development` criado e configurado
- [ ] Realm `production` criado e configurado
- [ ] Roles criadas (system-admin, org-admin, proponente, parecerista, investidor)
- [ ] Groups/Organizações criadas
- [ ] Client `apporte-frontend-dev` configurado
- [ ] Client `workflow-engine-dev` configurado
- [ ] Client scope `organization` configurado e associado
- [ ] Usuários de teste criados com roles e atributos

### Backend
- [ ] Dependência `quarkus-oidc` adicionada
- [ ] `application-dev-keycloak.properties` configurado
- [ ] Client secret configurado
- [ ] Classes de segurança criadas (KeycloakUserContext, OrganizationFilter)
- [ ] Endpoints de teste criados
- [ ] Backend inicia sem erros
- [ ] Endpoint `/q/health` retorna 200
- [ ] Endpoint `/api/auth/me` retorna 401 sem token
- [ ] Endpoint `/api/auth/me` retorna 200 com token válido
- [ ] Controle de acesso por role funcionando

### Frontend
- [ ] Biblioteca `keycloak-js` instalada
- [ ] `keycloak.ts` configurado
- [ ] `AuthContext` implementado
- [ ] `.env` configurado com variáveis corretas
- [ ] Login redireciona para Keycloak
- [ ] Após login, usuário volta para app
- [ ] Nome do usuário aparece na UI
- [ ] Token é enviado nas requisições à API
- [ ] Componentes protegidos por role funcionam
- [ ] Logout funciona corretamente

### Integração
- [ ] Frontend consegue fazer login via Keycloak
- [ ] Frontend recebe token JWT
- [ ] Frontend envia token para backend
- [ ] Backend valida token com Keycloak
- [ ] Backend extrai informações do usuário do token
- [ ] Backend aplica controle de acesso por role
- [ ] Logout em um serviço desloga em todos (SSO)

---

## 🎉 Conclusão

Se todos os itens do checklist estão ✅, sua integração Keycloak está completa e funcionando!

**Próximos passos**:
1. Implementar funcionalidades reais da aplicação
2. Deploy do backend no Kubernetes
3. Deploy do frontend (Replit ou Vercel)
4. Configurar ambiente de produção
5. Adicionar mais organizações e usuários

---

## 📚 Recursos Adicionais

- **Keycloak Admin Console**: https://auth.apporte.work/admin
- **Documentação Keycloak**: https://www.keycloak.org/documentation
- **Quarkus OIDC Guide**: https://quarkus.io/guides/security-oidc-bearer-token-authentication
- **Keycloak JS Adapter**: https://www.keycloak.org/docs/latest/securing_apps/#_javascript_adapter

---

**Dúvidas?** Consulte os logs e a documentação!
