# Guia de Migração - Keycloak + Java 21 + Clean Code

## 🎯 O que foi feito

### ✅ 1. UserContext modernizado (Java 21 Record)

**Antes** (classe tradicional):
```java
public class UserContext {
    private String id;
    private String email;
    private String name;
    private List<String> roles;
    // + getters/setters + equals/hashCode + toString
}
```

**Depois** (record moderno):
```java
public record UserContext(
    String id,
    String email,
    String name,
    String organizationId,        // ✨ NOVO: Multi-tenant
    String organizationName,       // ✨ NOVO: Multi-tenant
    Set<String> roles              // ✨ Set é mais eficiente
) {
    // Factory method para integração Keycloak
    public static UserContext fromKeycloak(KeycloakUserContext keycloak) {...}
    
    // Métodos auxiliares modernos
    public boolean hasRole(String role) {...}
    public boolean isSystemAdmin() {...}
    public Optional<String> organization() {...}
}
```

**Benefícios**:
- ✅ Imutável por padrão (thread-safe)
- ✅ Menos código boilerplate
- ✅ Suporte a organização (multi-tenant)
- ✅ equals/hashCode/toString automáticos
- ✅ Validação no compact constructor

---

### ✅ 2. WorkflowController modernizado

**Antes**:
```java
@Inject
private JwtValidator jwtValidator;

@GET
@Path("/{id}")
public Response get(@Context HttpHeaders headers) {
    UserContext user = extractUserContext(headers);  // Manual
    // Sem validação de organização
    // Sem @Authenticated
}
```

**Depois**:
```java
@Authenticated  // ✨ Keycloak valida automaticamente
@Inject
private final KeycloakUserContext keycloakUserContext;

@GET
@Path("/{pipelineId}")
public Response getPipeline(@PathParam("pipelineId") Long pipelineId) {
    var userContext = UserContext.fromKeycloak(keycloakUserContext);
    validateOrganizationAccess(pipeline.getOrganizationId(), userContext);
    // Validação de organização integrada
}

@POST
@Path("/{pipelineId}/cards/{cardId}/move")
@RolesAllowed({"proponente", "org-admin", "system-admin"})  // ✨ Role-based
public Response moveCard(...) {...}
```

**Melhorias**:
- ✅ Injeção de dependência via construtor (melhor testabilidade)
- ✅ Java 21 `var` para inferência de tipos
- ✅ Records para DTOs (PipelineResponse, ErrorResponse)
- ✅ Validação de organização automática
- ✅ @RolesAllowed para controle fino de acesso
- ✅ Logging estruturado com contexto
- ✅ Documentação Javadoc completa

---

## 🔧 O que você precisa fazer

### 1️⃣ Atualizar model Pipeline (adicionar organizationId)

```java
// src/main/java/com/apporte/domain/model/Pipeline.java

@Entity
@Table(name = "pipelines")
public class Pipeline extends PanacheEntity {
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "organization_id", nullable = false)  // ✨ ADICIONAR
    private String organizationId;
    
    // Getters e setters
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
}
```

### 2️⃣ Migração do banco de dados

```sql
-- Adicionar coluna organization_id
ALTER TABLE pipelines 
ADD COLUMN organization_id VARCHAR(255);

-- Para dados existentes, você pode:
-- Opção A: Definir uma organização padrão
UPDATE pipelines 
SET organization_id = 'org-default-001' 
WHERE organization_id IS NULL;

-- Opção B: Mapear pipelines por criador (se tiver tabela de usuários)
-- UPDATE pipelines p
-- SET organization_id = u.organization_id
-- FROM users u
-- WHERE p.created_by = u.id;

-- Tornar NOT NULL após preencher
ALTER TABLE pipelines 
ALTER COLUMN organization_id SET NOT NULL;

-- Criar índice para performance
CREATE INDEX idx_pipelines_organization_id 
ON pipelines(organization_id);
```

### 3️⃣ Remover JwtValidator antigo

```bash
# O JwtValidator não é mais necessário, Keycloak faz isso
rm src/main/java/com/apporte/security/JwtValidator.java
```

### 4️⃣ Atualizar application.properties

```bash
# Escolha o profile correto
cd /home/joaopedro/workflow-engine

# Para development
cp src/main/resources/application.properties src/main/resources/application.properties.backup
cat src/main/resources/application-dev-keycloak.properties >> src/main/resources/application.properties

# OU criar novo perfil no application.properties
echo "quarkus.profile=dev-keycloak" >> src/main/resources/application.properties
```

### 5️⃣ Configurar Client Secret

```bash
# Obter do Keycloak
# Admin Console → Clients → workflow-engine-dev → Credentials → Client Secret

# Editar e adicionar
nano src/main/resources/application-dev-keycloak.properties
# quarkus.oidc.credentials.secret=SEU_CLIENT_SECRET_AQUI
```

---

## 🧪 Como testar

### 1. Compilar

```bash
cd /home/joaopedro/workflow-engine
./mvnw clean compile
```

### 2. Iniciar com perfil Keycloak

```bash
./mvnw quarkus:dev -Dquarkus.profile=dev-keycloak
```

### 3. Obter token e testar

```bash
# Obter token
export TOKEN=$(curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | jq -r '.access_token')

# Testar endpoint de autenticação
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/me | jq '.'

# Testar endpoint de pipeline (assumindo que existe pipeline ID 1)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/pipelines/1 | jq '.'
```

---

## 📊 Comparação: Antes vs Depois

### Segurança

| Aspecto | Antes | Depois |
|---------|-------|--------|
| Autenticação | JWT manual | Keycloak OIDC automático |
| Validação | JwtValidator custom | Quarkus @Authenticated |
| Roles | Verificação manual | @RolesAllowed declarativo |
| Multi-tenant | ❌ Não suportado | ✅ Validação por org_id |
| Token refresh | ❌ Manual | ✅ Automático (Keycloak) |

### Código

| Aspecto | Antes | Depois |
|---------|-------|--------|
| UserContext | Classe (50 linhas) | Record (40 linhas) |
| Controller | Imperativo | Declarativo + moderno |
| DTOs | Classes aninhadas | Records (Java 21) |
| Error handling | Try-catch básico | Estruturado com logs |
| Injeção | @Inject em fields | Constructor injection |
| Type inference | Tipos explícitos | `var` (Java 21) |

### Performance

| Aspecto | Antes | Depois |
|---------|-------|--------|
| Token parsing | A cada request | Cache do Keycloak |
| Validação roles | Loop manual | Set.contains() O(1) |
| Immutability | ❌ Mutável | ✅ Imutável (records) |
| Thread-safety | ⚠️ Potencial issue | ✅ Thread-safe |

---

## 🎯 Próximos passos recomendados

### Curto prazo (fazer agora)

1. ✅ Adicionar `organizationId` ao modelo `Pipeline`
2. ✅ Criar migração SQL para adicionar coluna
3. ✅ Obter client secret e configurar
4. ✅ Testar endpoints com token Keycloak
5. ✅ Remover `JwtValidator.java` antigo

### Médio prazo (próxima sprint)

1. ⏳ Atualizar outros controllers (se existirem) com mesmo padrão
2. ⏳ Adicionar testes unitários para UserContext
3. ⏳ Adicionar testes de integração com Keycloak
4. ⏳ Criar DTOs para requests/responses (evitar expor entidades)
5. ⏳ Implementar paginação nos endpoints de listagem

### Longo prazo (features futuras)

1. 📋 Adicionar audit log (quem fez o quê, quando)
2. 📋 Implementar filtros por organização nas queries
3. 📋 Adicionar webhook events para ações importantes
4. 📋 Rate limiting por organização
5. 📋 Métricas por organização (Prometheus)

---

## 🐛 Possíveis problemas

### Erro: "Pipeline.getOrganizationId() não existe"

**Solução**: Você precisa adicionar o campo no modelo Pipeline (passo 1️⃣)

### Erro: "NullPointerException em validateOrganizationAccess"

**Solução**: Certifique-se de que:
1. Todos os pipelines têm `organization_id` preenchido no banco
2. Usuários Keycloak têm atributos `organization_id` configurados

### Erro: "OIDC Server is not available"

**Solução**:
```bash
# Verificar conectividade
curl https://auth.apporte.work/realms/development/.well-known/openid-configuration

# Verificar client secret
grep "credentials.secret" src/main/resources/application-dev-keycloak.properties
```

### Compilação falha: "cannot find symbol: var"

**Solução**: Certifique-se de estar usando Java 21:
```bash
java -version  # Deve mostrar Java 21+
```

No `pom.xml`:
```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

---

## 📚 Recursos adicionais

- **Keycloak Setup**: `docs/keycloak/keycloak-setup-guide.md`
- **Backend Integration**: `docs/keycloak/keycloak-backend-integration.md`
- **Testing Guide**: `docs/keycloak/keycloak-testing-guide.md`
- **Commands Reference**: `docs/keycloak/keycloak-commands.md`
- **Java 21 Records**: https://docs.oracle.com/en/java/javase/21/language/records.html
- **Quarkus Security**: https://quarkus.io/guides/security-oidc-bearer-token-authentication

---

## ✅ Checklist de migração

- [ ] UserContext modernizado (record) ✅ FEITO
- [ ] WorkflowController atualizado ✅ FEITO
- [ ] Pipeline.organizationId adicionado
- [ ] Migração SQL executada
- [ ] Client secret configurado
- [ ] JwtValidator removido
- [ ] Testes executados com sucesso
- [ ] Documentação atualizada
- [ ] Code review realizado
- [ ] Deploy em dev

---

**Dúvidas?** Consulte a documentação completa em `docs/keycloak/` 🚀
