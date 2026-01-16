# Integração Backend (workflow-engine) com Keycloak

## 🎯 O que foi feito

### 1. Dependências Adicionadas

Adicionado ao `pom.xml`:
- `quarkus-oidc` - Integração OpenID Connect com Keycloak
- `quarkus-security` - Framework de segurança do Quarkus

### 2. Arquivos de Configuração

Criados dois arquivos de properties:

#### `application-dev-keycloak.properties`
- Realm: `development`
- Client: `workflow-engine-dev`
- CORS liberado para localhost e Replit
- Logging em DEBUG para facilitar troubleshooting

#### `application-prod-keycloak.properties`
- Realm: `production`
- Client: `workflow-engine-prod`
- CORS apenas para domínios apporte.work
- Client secret via variável de ambiente
- Logging em INFO/WARN

### 3. Classes Java Criadas

#### `KeycloakUserContext.java`
Classe utilitária que extrai informações do JWT:
- Dados do usuário (ID, email, nome)
- Roles e permissões
- Informações da organização (org_id, org_name)
- Métodos helper (isSystemAdmin, isProponente, etc.)

#### `OrganizationFilter.java`
Filtro que valida multi-tenancy:
- Verifica se usuário pertence à organização
- Admin do sistema pode acessar qualquer organização
- Lê header `X-Organization-ID` das requisições

#### `AuthTestController.java`
Controller de exemplo com endpoints protegidos:
- `/api/auth/public` - Público
- `/api/auth/me` - Qualquer usuário autenticado
- `/api/auth/admin` - Apenas system-admin
- `/api/auth/proponente` - Apenas proponentes
- `/api/auth/parecerista` - Apenas pareceristas
- `/api/auth/investidor` - Apenas investidores

---

## 🚀 Como Usar

### Passo 1: Obter Client Secret do Keycloak

1. Acesse: https://auth.apporte.work/admin
2. Selecione realm `development`
3. Vá em **Clients** → `workflow-engine-dev`
4. Vá na aba **Credentials**
5. Copie o **Client Secret**

### Passo 2: Configurar o Application Properties

Edite `application-dev-keycloak.properties`:

```properties
quarkus.oidc.credentials.secret=SEU_CLIENT_SECRET_AQUI
```

### Passo 3: Escolher Profile de Execução

Você tem duas opções:

#### Opção A: Usar profile Keycloak (RECOMENDADO)

```bash
# Development com Keycloak
./mvnw quarkus:dev -Dquarkus.profile=dev-keycloak

# Ou adicionar ao application.properties:
quarkus.profile=dev-keycloak
```

#### Opção B: Mesclar configurações no application.properties

Copie as configurações de `application-dev-keycloak.properties` para `application.properties`.

---

## 🧪 Como Testar

### Teste 1: Obter Token JWT

```bash
# Obter token de um usuário
curl -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password'
```

**Salve o `access_token` retornado!**

### Teste 2: Chamar Endpoint Público

```bash
curl http://localhost:8081/api/auth/public
```

**Resultado esperado**: `{"message": "Este endpoint é público", ...}`

### Teste 3: Chamar Endpoint Protegido (sem token)

```bash
curl http://localhost:8081/api/auth/me
```

**Resultado esperado**: HTTP 401 Unauthorized

### Teste 4: Chamar Endpoint Protegido (com token)

```bash
# Substitua YOUR_ACCESS_TOKEN pelo token obtido no Teste 1
curl http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Resultado esperado**: JSON com dados do usuário

```json
{
  "userId": "...",
  "email": "proponente@exemplo.com",
  "name": "João Proponente",
  "organizationId": "org-exemplo-001",
  "organizationName": "Organização Exemplo",
  "roles": ["proponente"],
  "isProponente": true,
  ...
}
```

### Teste 5: Endpoint Específico de Role

```bash
# Deve funcionar (proponente tem acesso)
curl http://localhost:8081/api/auth/proponente \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Deve retornar 403 Forbidden (proponente não é admin)
curl http://localhost:8081/api/auth/admin \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Teste 6: Debug Token Info

```bash
curl http://localhost:8081/api/auth/debug \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 💡 Como Proteger Seus Próprios Endpoints

### Exemplo 1: Endpoint que requer autenticação

```java
import io.quarkus.security.Authenticated;

@GET
@Path("/meu-endpoint")
@Authenticated  // Qualquer usuário autenticado
public Response meuEndpoint() {
    return Response.ok("Protegido!").build();
}
```

### Exemplo 2: Endpoint com role específica

```java
import jakarta.annotation.security.RolesAllowed;

@GET
@Path("/criar-proposta")
@RolesAllowed({"proponente", "org-admin"})  // Apenas essas roles
public Response criarProposta() {
    return Response.ok("Proposta criada!").build();
}
```

### Exemplo 3: Acessar dados do usuário

```java
import com.apporte.security.KeycloakUserContext;
import jakarta.inject.Inject;

@Inject
KeycloakUserContext userContext;

@GET
@Path("/minhas-propostas")
@Authenticated
public Response minhasPropostas() {
    String userId = userContext.getUserId();
    String orgId = userContext.getOrganizationId().orElse(null);
    
    // Buscar propostas do usuário na organização
    List<Proposta> propostas = propostaService.findByUserAndOrg(userId, orgId);
    
    return Response.ok(propostas).build();
}
```

### Exemplo 4: Validação customizada

```java
@POST
@Path("/aprovar-proposta/{id}")
@RolesAllowed("parecerista")
public Response aprovarProposta(@PathParam("id") String propostaId) {
    // Verificar se parecerista pertence à mesma organização da proposta
    Proposta proposta = propostaService.findById(propostaId);
    
    if (!userContext.belongsToOrganization(proposta.getOrganizationId())) {
        return Response.status(403)
                .entity("Você não pode aprovar propostas de outra organização")
                .build();
    }
    
    // Aprovar proposta...
    return Response.ok().build();
}
```

---

## 🔐 Boas Práticas de Segurança

### 1. Sempre valide organização em operações sensíveis

```java
if (!userContext.isSystemAdmin() && 
    !userContext.belongsToOrganization(requestedOrgId)) {
    throw new ForbiddenException("Access denied");
}
```

### 2. Use hierarquia de roles

```java
// Admin de organização pode fazer tudo que proponente faz
@RolesAllowed({"proponente", "org-admin", "system-admin"})
```

### 3. Nunca confie apenas no frontend

Mesmo que o frontend bloqueie um botão, o backend DEVE validar permissões.

### 4. Log de acessos sensíveis

```java
Log.info(String.format("Proposta %s aprovada por %s (%s)", 
        propostaId, 
        userContext.getEmail(),
        userContext.getOrganizationName().orElse("N/A")
));
```

### 5. Use HTTPS em produção

No Kubernetes, configure TLS/SSL para todas as comunicações.

---

## 🐛 Troubleshooting

### Erro: "OIDC Server is not available"

**Solução**: Verifique se o Keycloak está acessível:

```bash
curl https://auth.apporte.work/realms/development/.well-known/openid-configuration
```

### Erro: "Invalid token"

**Soluções**:
1. Token expirado? Obtenha um novo token
2. Client secret incorreto? Verifique no Keycloak
3. Realm errado? Verifique a URL do OIDC

### Erro: "Access denied" mesmo com role correta

**Solução**: Verifique se o role claim path está correto:

```properties
quarkus.oidc.roles.role-claim-path=realm_access/roles
```

### CORS errors no frontend

**Solução**: Adicione a URL do Replit no `application-dev-keycloak.properties`:

```properties
quarkus.http.cors.origins=/regex_do_replit/,http://localhost:3000
```

---

## 📊 Monitoramento

### Health Check

```bash
curl http://localhost:8081/q/health
```

### Métricas

```bash
curl http://localhost:8081/q/metrics
```

---

## ✅ Checklist de Deploy

Antes de fazer deploy do backend:

- [ ] Client secret configurado via variável de ambiente
- [ ] Profile correto (dev-keycloak ou prod-keycloak)
- [ ] CORS configurado para domínios corretos
- [ ] Keycloak acessível do cluster K8s
- [ ] Database configurado corretamente
- [ ] Health checks funcionando

---

**🎉 Backend integrado com Keycloak!**
