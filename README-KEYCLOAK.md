# 🔐 Keycloak Authentication - Workflow Engine

## 📍 Localização da Documentação

Toda a documentação de integração com Keycloak está em:

```
workflow-engine/
├── docs/keycloak/
│   ├── KEYCLOAK-README.md              # 🚀 COMECE AQUI
│   ├── keycloak-setup-guide.md         # Configuração manual do Keycloak
│   ├── keycloak-backend-integration.md # Integração Quarkus + OIDC
│   ├── keycloak-frontend-react.md      # Integração React (para o frontend)
│   ├── keycloak-testing-guide.md       # Testes e troubleshooting
│   └── keycloak-commands.md            # Comandos úteis
│
├── scripts/
│   └── test-keycloak-integration.sh    # Script de testes automatizados
│
└── src/main/
    ├── java/com/apporte/
    │   ├── security/
    │   │   ├── KeycloakUserContext.java     # Context de usuário
    │   │   └── OrganizationFilter.java      # Validação multi-tenant
    │   └── controller/
    │       └── AuthTestController.java      # Endpoints de teste
    │
    └── resources/
        ├── application-dev-keycloak.properties  # Config dev
        └── application-prod-keycloak.properties # Config prod
```

---

## 🚀 Quick Start

### 1. Ler Documentação Principal

```bash
cat docs/keycloak/KEYCLOAK-README.md
```

### 2. Configurar Keycloak (30 min)

Acesse: https://auth.apporte.work/admin

Siga o guia completo:
```bash
cat docs/keycloak/keycloak-setup-guide.md
```

### 3. Configurar Backend (20 min)

```bash
# 1. Obter client secret do Keycloak
# Admin Console → Clients → workflow-engine-dev → Credentials

# 2. Editar configuração
nano src/main/resources/application-dev-keycloak.properties
# Substituir: quarkus.oidc.credentials.secret=SEU_SECRET_AQUI

# 3. Iniciar backend
./mvnw quarkus:dev -Dquarkus.profile=dev-keycloak
```

### 4. Testar

```bash
# Obter token
export TOKEN=$(curl -s -X POST 'https://auth.apporte.work/realms/development/protocol/openid-connect/token' \
  -d 'client_id=apporte-frontend-dev' \
  -d 'username=proponente@exemplo.com' \
  -d 'password=Proponente@123' \
  -d 'grant_type=password' | jq -r '.access_token')

# Testar endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/me | jq '.'

# Executar todos os testes
./scripts/test-keycloak-integration.sh
```

---

## 🎓 Arquitetura

```
Browser
  ↓
React Frontend (Replit)
  ↓ (JWT Token)
Quarkus Backend (este projeto)
  ↓ (valida token)
Keycloak (https://auth.apporte.work)
```

**Multi-tenant**: Cada organização isolada com `org_id` no token JWT

**Roles**: System Admin, Org Admin, Proponente, Parecerista, Investidor

---

## 📚 Documentação Completa

| Documento | Descrição |
|-----------|-----------|
| **[KEYCLOAK-README.md](docs/keycloak/KEYCLOAK-README.md)** | Resumo executivo - comece aqui |
| **[keycloak-setup-guide.md](docs/keycloak/keycloak-setup-guide.md)** | Passo a passo da configuração |
| **[keycloak-backend-integration.md](docs/keycloak/keycloak-backend-integration.md)** | Como usar no código |
| **[keycloak-frontend-react.md](docs/keycloak/keycloak-frontend-react.md)** | Integração frontend |
| **[keycloak-testing-guide.md](docs/keycloak/keycloak-testing-guide.md)** | Testes e troubleshooting |
| **[keycloak-commands.md](docs/keycloak/keycloak-commands.md)** | Comandos úteis |

---

## 💡 Exemplo de Uso

### Proteger Endpoint

```java
import jakarta.annotation.security.RolesAllowed;
import com.apporte.security.KeycloakUserContext;
import jakarta.inject.Inject;

@Inject
KeycloakUserContext userContext;

@GET
@Path("/propostas")
@RolesAllowed({"proponente", "org-admin"})
public Response listarPropostas() {
    String orgId = userContext.getOrganizationId().orElse(null);
    // Retornar apenas propostas da organização do usuário
}
```

---

## 🔑 Credenciais de Teste (Development)

| Usuário | Senha | Role |
|---------|-------|------|
| admin@apporte.dev | Admin@123 | system-admin |
| proponente@exemplo.com | Proponente@123 | proponente |
| parecerista@exemplo.com | Parecerista@123 | parecerista |
| investidor@exemplo.com | Investidor@123 | investidor |

---

## ✅ Status

- [x] Dependências OIDC adicionadas
- [x] Arquivos de configuração criados
- [x] Classes de segurança implementadas
- [x] Endpoints de teste criados
- [x] Script de testes criado
- [x] Documentação completa
- [ ] Client secret configurado (fazer manualmente)
- [ ] Keycloak configurado (seguir guia)
- [ ] Integração testada

---

**Próximo passo**: Leia [docs/keycloak/KEYCLOAK-README.md](docs/keycloak/KEYCLOAK-README.md) 🚀
