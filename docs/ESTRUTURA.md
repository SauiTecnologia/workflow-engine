# Estrutura Final - Workflow Service

## 📁 Hierarquia de Arquivos

```
src/main/java/com/apporte/
│
├── 📁 controller/
│   └── WorkflowController.java       (REST API - endpoints)
│
├── 📁 service/
│   └── WorkflowService.java          (Orquestração - lógica de negócio)
│
├── 📁 domain/
│   ├── 📁 model/                     (Entidades JPA e Contexto)
│   │   ├── Pipeline.java
│   │   ├── PipelineColumn.java
│   │   ├── PipelineCard.java
│   │   └── UserContext.java
│   │
│   ├── 📁 event/                     (Observer Pattern)
│   │   ├── CardMovedEvent.java
│   │   ├── WorkflowEventListener.java
│   │   └── WorkflowEventManager.java
│   │
│   └── 📁 exception/                 (Exceções de domínio)
│       ├── WorkflowException.java
│       ├── UnauthorizedException.java
│       ├── InvalidTransitionException.java
│       ├── InvalidEntityTypeException.java
│       ├── InvalidInputException.java
│       └── UnsupportedEntityTypeException.java
│
├── 📁 validator/                     (Strategy Pattern)
│   ├── PermissionValidator.java      (Valida roles)
│   ├── TransitionValidator.java      (Valida transições)
│   └── EntityTypeValidator.java      (Valida tipos de entidade)
│
├── 📁 command/                       (Command Pattern)
│   ├── WorkflowCommand.java          (Interface)
│   ├── MoveCardCommand.java          (Implementação)
│   ├── CommandResult.java            (Resultado)
│   └── CommandExecutor.java          (Executor com histórico)
│
├── 📁 repository/                    (Repository Pattern - DAO)
│   ├── PipelineRepository.java
│   ├── PipelineColumnRepository.java
│   └── PipelineCardRepository.java
│
├── 📁 dto/                           (Data Transfer Objects)
│   ├── MoveCardRequest.java
│   ├── MoveCardResponse.java
│   └── NotificationPayload.java
│
├── 📁 security/
│   └── JwtValidator.java             (Validação JWT Supabase)
│
└── 📁 health/
    └── MyLivenessCheck.java          (Health check)
```

---

## 🔢 Estatísticas

- **Classes Totais:** 30
- **Padrões de Projeto:** 5 (Repository, Strategy, Command, Observer, Circuit Breaker)
- **Entidades JPA:** 3 (Pipeline, PipelineColumn, PipelineCard)
- **Validators:** 3 (PermissionValidator, TransitionValidator, EntityTypeValidator)
- **Exceções Customizadas:** 5

---

## 📊 Mapa de Dependências

```
WorkflowController
    ↓
├─ JwtValidator
└─ WorkflowService
    ├─ PipelineRepository
    ├─ PipelineColumnRepository
    ├─ PipelineCardRepository
    ├─ CommandExecutor
    │   └─ MoveCardCommand
    │       ├─ PermissionValidator
    │       ├─ TransitionValidator
    │       └─ EntityTypeValidator
    └─ WorkflowEventManager
        └─ WorkflowEventListener (observadores)
```

---

## 🎯 Responsabilidades por Camada

### Controller Layer
- `WorkflowController` - Receber requisições HTTP, extrair JWT, chamar service

### Service Layer
- `WorkflowService` - Orquestra operações, valida regras de negócio

### Domain Layer
- `Pipeline`, `PipelineColumn`, `PipelineCard` - Modelos persistentes
- `UserContext` - Contexto do usuário autenticado
- `CardMovedEvent` - Evento de domínio
- 5 Exceções customizadas - Erros específicos de negócio

### Validation Layer (Strategies)
- `PermissionValidator` - Validar roles/permissões
- `TransitionValidator` - Validar transições entre colunas
- `EntityTypeValidator` - Validar tipos de entidade permitidos

### Command Layer
- `MoveCardCommand` - Encapsula movimento de card com validações
- `CommandExecutor` - Executa com histórico

### Event Layer (Observer)
- `WorkflowEventManager` - Gerencia listeners
- `WorkflowEventListener` - Interface para observers

### Repository Layer
- `PipelineRepository` - Acesso a dados de Pipeline
- `PipelineColumnRepository` - Acesso a dados de Column
- `PipelineCardRepository` - Acesso a dados de Card

### DTO Layer
- `MoveCardRequest` - Input JSON
- `MoveCardResponse` - Output JSON
- `NotificationPayload` - Payload para Notification Service

### Security Layer
- `JwtValidator` - Valida e extrai UserContext do JWT

---

## ✨ O Que Foi Removido

- ❌ `GreetingController` - Controller de exemplo
- ❌ `GreetingResource` - Resource de exemplo
- ❌ `GreetingService` - Serviço de exemplo
- ❌ `MyEntity` - Entidade de exemplo
- ❌ `MyEntityDAO` - DAO de exemplo
- ❌ Todos os testes de exemplo

---

## 📝 Padrões Implementados

| Padrão | Localização | Propósito |
|--------|-------------|----------|
| **Repository** | `repository/` | Abstração de dados (DAO) |
| **Strategy** | `validator/` | Validações dinâmicas plugáveis |
| **Command** | `command/` | Encapsular operações com histórico |
| **Observer** | `domain/event/` | Notificações desacopladas |
| **Circuit Breaker** | `client/` | Resiliência (Quarkus FT) |

---

## 🚀 Fluxo Completo: Mover Card

```
1. POST /api/pipelines/{pipelineId}/cards/{cardId}/move
   ↓
2. WorkflowController.moveCard()
   ├─ Extrair JWT → UserContext
   ├─ Validar autorização básica
   ├─ Chamar WorkflowService.moveCard()
   ↓
3. WorkflowService.moveCard()
   ├─ Buscar card e validar existência
   ├─ Criar MoveCardCommand
   ├─ CommandExecutor.execute(command)
   ↓
4. MoveCardCommand.execute()
   ├─ PermissionValidator.canMoveOut()  ✓
   ├─ PermissionValidator.canMoveIn()   ✓
   ├─ TransitionValidator.validateTransition()  ✓
   ├─ EntityTypeValidator.validateEntityType()  ✓
   ├─ Update database
   ├─ Retornar CommandResult
   ↓
5. WorkflowEventManager.fireCardMoved()
   ├─ NotificationDispatcher.onCardMoved() [próxima implementação]
   └─ AuditEventDispatcher.onCardMoved() [próxima implementação]
   ↓
6. HTTP 200 OK
   {
     id: "card-1",
     columnId: "col-2",
     entityType: "project",
     entityId: "proj-1"
   }
```

---

## 📚 Documentação

- **WORKFLOW_SERVICE.md** - Documentação principal (este arquivo)
- **README.md** - Documentação geral do projeto

---

## ✅ Status da Implementação

### Fase 1: Fundação ✅
- [x] Entidades JPA
- [x] Repositories
- [x] Exceções de domínio
- [x] Validadores (Strategies)
- [x] Command Pattern
- [x] Event System

### Fase 2: Serviço ✅
- [x] WorkflowService
- [x] MoveCardCommand
- [x] Validações em cascata

### Fase 3: API ✅
- [x] WorkflowController
- [x] DTOs
- [x] JwtValidator

### Fase 4: Próximas (Em desenvolvimento)
- [ ] NotificationDispatcher
- [ ] AuditEventDispatcher
- [ ] Mappers completos
- [ ] Testes unitários
- [ ] Testes de integração

---

## 🔗 Endpoints Implementados

### GET /api/pipelines/{pipelineId}
Carrega um pipeline com suas colunas e cards

**Headers:**
```
Authorization: Bearer {jwt}
```

**Response:**
```json
{
  "pipeline": {...},
  "columns": [...]
}
```

### POST /api/pipelines/{pipelineId}/cards/{cardId}/move
Move um card de uma coluna para outra

**Headers:**
```
Authorization: Bearer {jwt}
```

**Body:**
```json
{
  "fromColumnId": "col-1",
  "toColumnId": "col-2"
}
```

**Response:**
```json
{
  "id": "card-1",
  "columnId": "col-2",
  "entityType": "project",
  "entityId": "proj-1",
  "sortOrder": 0
}
```

---

## 💡 Design Decisions

1. **Panache com extends PanacheEntity** - Simplifica CRUD, menos boilerplate
2. **JSONB para configurações dinâmicas** - Transições, regras, layouts em JSON
3. **Strategy Pattern para validators** - Regras sem if/else, fácil estender
4. **Command Pattern para movimento** - Histórico automático, suporte a undo
5. **Observer Pattern para eventos** - Desacoplamento entre componentes
6. **Validações em cascata** - Entrada → Autorização → Negócio

---

## 🛠️ Tecnologias

- **Quarkus** - Framework Java nativo/GraalVM
- **Hibernate + Panache** - ORM simplificado
- **PostgreSQL** - Banco (Supabase)
- **Jakarta EE** - Standards (formerly Java EE)
- **Quarkus Fault Tolerance** - Circuit Breaker, Retry

