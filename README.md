# 🚀 Workflow Service - Motor de Kanban

Microserviço Quarkus que gerencia um **Kanban configurável** com suporte a múltiplos tipos de entidades, regras dinâmicas baseadas em JSON e integração com serviços de notificação.

## ✨ Características Principais

- **Arquitetura em Camadas** - Controller → Service → Command → Repository
- **5 Design Patterns** - Repository, Strategy, Command, Observer, Circuit Breaker
- **Validações em Cascata** - 5 níveis de validação (entrada, autorização, negócio, execução, notificação)
- **Configurável via JSON** - Transições, permissões e layouts definidos no banco
- **Histórico de Operações** - Via Command Pattern
- **Event-Driven** - Observer Pattern para notificações desacopladas
- **Type-Safe** - Exceções customizadas específicas
- **Resiliente** - Circuit Breaker para falhas de serviços externos

## 📊 Estrutura

```
30 classes Java
├── 3 Entidades JPA
├── 3 Repositories
├── 3 Validadores (Strategies)
├── 4 Comandos
├── 3 Eventos
├── 1 Serviço
├── 1 Controller
├── 6 Exceções
├── 3 DTOs
└── 1 Segurança
```

## 🗄️ Entidades

### Pipeline
Kanban board para um contexto específico (ex: edital-123)

### PipelineColumn
Coluna com regras dinâmicas em JSONB:
- `transition_rules_json` - Transições permitidas
- `notification_rules_json` - Regras de notificação
- `card_layout_json` - Layout do card
- `allowed_roles_*` - Permissões por role

### PipelineCard
Card representando uma entidade (projeto, avaliação, etc)

## 🔄 Fluxo: Mover Card

```
POST /api/pipelines/{id}/cards/{cardId}/move
  ↓
WorkflowController (JWT) → WorkflowService
  ↓
MoveCardCommand.execute()
  ├─ PermissionValidator.canMoveOut() ✓
  ├─ PermissionValidator.canMoveIn() ✓
  ├─ TransitionValidator.validateTransition() ✓
  ├─ EntityTypeValidator.validateEntityType() ✓
  ├─ CardRepository.persist()
  ↓
WorkflowEventManager
  ├─ NotificationDispatcher.onCardMoved()
  └─ AuditEventDispatcher.onCardMoved()
  ↓
HTTP 200 OK
```

## 🚀 Quick Start (Com Supabase)

### 1. Configurar Variáveis de Ambiente

```bash
# Copiar template
cp .env.example .env

# Editar com suas credenciais Supabase
nano .env
```

Variáveis necessárias:
```env
SUPABASE_PROJECT_ID=seu_project_id
SUPABASE_PASSWORD=sua_database_password
```

👉 Veja [SUPABASE_CREDENTIALS_GUIDE.md](./SUPABASE_CREDENTIALS_GUIDE.md) para obter credenciais

### 2. Carregar Variáveis

```bash
source .env  # Linux/Mac
# ou configure manualmente no Windows
```

### 3. Criar Schema no Supabase

```bash
# 1. Acesse https://app.supabase.com
# 2. Vá para SQL Editor
# 3. Execute o script em SUPABASE_SETUP.md (seção 4.2)
```

### 4. Rodar em Desenvolvimento

```bash
./mvnw quarkus:dev
```

Esperado:
```
Listening on: http://localhost:8080
Connection to PostgreSQL successful
```

### 5. Testar

```bash
# Verificar saúde
curl http://localhost:8080/q/health/live

# Chamar API (requer JWT)
curl -X GET http://localhost:8080/api/pipelines/1 \
  -H "Authorization: Bearer {seu_jwt_token}"
```

👉 Veja [QUICKSTART_SUPABASE.md](./QUICKSTART_SUPABASE.md) para guia em 5 minutos

## 📚 Documentação

| Documento | Descrição |
|-----------|-----------|
| **SUMARIO.md** | 📋 Resumo executivo |
| **WORKFLOW_SERVICE.md** | 📖 Documentação técnica completa |
| **ESTRUTURA.md** | 🗂️ Mapa de arquivos e responsabilidades |
| **GETTING_STARTED.md** | 🚀 Como executar e troubleshooting |
| **FILES_MANIFEST.md** | 📦 Manifesto detalhado de arquivos |
| **QUICKSTART_SUPABASE.md** | ⚡ Quick Start em 5 minutos |
| **SUPABASE_SETUP.md** | 🔧 Configuração completa do Supabase |
| **SUPABASE_CREDENTIALS_GUIDE.md** | 🔑 Como obter credenciais Supabase |
| **SUPABASE_CONFIG_SUMMARY.md** | 📊 Resumo de configuração |
| **CODE_QUALITY_REPORT.md** | ✅ Análise de qualidade de código |
| **DEVELOPMENT_GUIDE.md** | 👨‍💻 Guia de desenvolvimento |

## 🎯 Padrões Implementados

### Repository Pattern
- `PipelineRepository` - CRUD + queries
- `PipelineColumnRepository` - Buscar colunas
- `PipelineCardRepository` - Buscar cards

### Strategy Pattern
- `PermissionValidator` - Validar roles
- `TransitionValidator` - Validar transições (JSON)
- `EntityTypeValidator` - Validar tipos de entidade

### Command Pattern
- `MoveCardCommand` - Encapsula movimento com validações
- `CommandExecutor` - Executor com histórico
- `CommandResult` - Resultado com sucesso/erro

### Observer Pattern
- `WorkflowEventManager` - Gerenciador de eventos
- `WorkflowEventListener` - Interface para observadores
- `CardMovedEvent` - Evento disparado

## 📦 Build

### Desenvolvimento
```bash
./mvnw quarkus:dev
```

### Produção (JVM)
```bash
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

### Nativo (GraalVM)
```bash
./mvnw package -Pnative
./target/workflow-engine-1.0.0-SNAPSHOT-runner
```

### Docker
```bash
docker build -f src/main/docker/Dockerfile.jvm -t workflow-service:latest .
docker run -p 8080:8080 workflow-service:latest
```

## 🔐 Segurança

- JWT validation via `JwtValidator`
- Role-based access control
- Validações em múltiplas camadas

## 🧪 Testes

Em desenvolvimento. Estrutura preparada para:
- Testes unitários (Service, Validator, Command)
- Testes de integração (Controller, Repository)
- Testes E2E (Fluxo completo)

## 📈 Status

✅ **Implementado:**
- Arquitetura em camadas
- Entidades JPA e Repositories
- Validadores (Strategies)
- Command Pattern
- Event System
- REST API
- Security framework
- SQL schema com dados

🔜 **Próximo:**
- NotificationDispatcher
- AuditEventDispatcher
- Testes
- CI/CD

## 🛠️ Tecnologias

- **Framework:** Quarkus
- **ORM:** Hibernate + Panache
- **Banco:** PostgreSQL (Supabase)
- **Padrões:** Repository, Strategy, Command, Observer, Circuit Breaker
- **Java:** 17+

## 📞 Documentação

Para dúvidas, consulte:
- `SUMARIO.md` - Resumo executivo
- `WORKFLOW_SERVICE.md` - Detalhes técnicos
- `GETTING_STARTED.md` - Como executar

---

**Última atualização:** 22 de dezembro de 2025  
**Status:** Pronto para desenvolvimento  
**Tempo até produção:** 3-5 dias com equipe de 2 devs
