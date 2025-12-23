# Workflow Service - Motor de Kanban

## 📋 Visão Geral

Microserviço Quarkus que gerencia um Kanban configurável com suporte a:
- Múltiplos tipos de entidades (projetos, avaliações, etc)
- Regras dinâmicas baseadas em JSON (transições, permissões, notificações)
- Validação em camadas (entrada → autorização → negócio)
- Histórico de operações e auditoria
- Integração com Notification Service

---

## 🏗️ Arquitetura

```
REST Controller
     ↓
Service Layer (Orquestração)
     ↓
Command + Validators (Regras)
     ↓
Repository (Data Access)
     ↓
Database (PostgreSQL)
```

---

## 📦 Estrutura do Projeto

```
src/main/java/com/apporte/
├── controller/
│   └── WorkflowController.java       # REST API
│
├── service/
│   └── WorkflowService.java          # Orquestração
│
├── domain/
│   ├── model/
│   │   ├── Pipeline.java             # Entidade JPA
│   │   ├── PipelineColumn.java       # Entidade JPA
│   │   ├── PipelineCard.java         # Entidade JPA
│   │   └── UserContext.java          # Contexto do usuário
│   │
│   ├── event/
│   │   ├── CardMovedEvent.java       # Evento disparado
│   │   ├── WorkflowEventListener.java
│   │   └── WorkflowEventManager.java # Observer Pattern
│   │
│   └── exception/
│       ├── WorkflowException.java    # Base
│       ├── UnauthorizedException.java
│       ├── InvalidTransitionException.java
│       ├── InvalidEntityTypeException.java
│       └── InvalidInputException.java
│
├── validator/
│   ├── PermissionValidator.java      # Strategy: Validar roles
│   ├── TransitionValidator.java      # Strategy: Validar transições
│   └── EntityTypeValidator.java      # Strategy: Validar tipos
│
├── command/
│   ├── WorkflowCommand.java          # Interface
│   ├── MoveCardCommand.java          # Implementação
│   ├── CommandResult.java            # Resultado
│   └── CommandExecutor.java          # Executor + Histórico
│
├── repository/
│   ├── PipelineRepository.java
│   ├── PipelineColumnRepository.java
│   └── PipelineCardRepository.java
│
├── dto/
│   ├── MoveCardRequest.java
│   ├── MoveCardResponse.java
│   └── NotificationPayload.java
│
└── security/
    └── JwtValidator.java             # Validação JWT do Supabase
```

---

## 🔄 Fluxo: Mover Card

```
1. HTTP POST /api/pipelines/{id}/cards/{cardId}/move
   Headers: Authorization: Bearer {jwt}
   Body: {fromColumnId, toColumnId}
   ↓
2. WorkflowController
   ├─ Extrair JWT → UserContext
   ├─ Chamar WorkflowService.moveCard()
   ↓
3. WorkflowService
   ├─ Buscar card e validar
   ├─ Criar MoveCardCommand
   ├─ Executar comando com CommandExecutor
   ↓
4. MoveCardCommand.execute()
   ├─ ✓ Validação de entrada
   ├─ ✓ Permissão: canMoveOut?
   ├─ ✓ Permissão: canMoveIn?
   ├─ ✓ Transição permitida?
   ├─ ✓ Entity type permitido?
   ├─ Se OK: atualizar database
   ↓
5. WorkflowEventManager
   ├─ Dispara CardMovedEvent
   ├─ NotificationDispatcher.onCardMoved()
   │  └─ Chama Notification Service
   ├─ AuditEventDispatcher.onCardMoved()
   │  └─ Registra em auditoria
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

## 🎯 Design Patterns Usados

### 1. Repository Pattern
- **Objetivo:** Abstração de dados
- **Classes:** `PipelineRepository`, `PipelineColumnRepository`, `PipelineCardRepository`
- **Benefício:** Fácil trocar BD, testar com mocks

### 2. Strategy Pattern
- **Objetivo:** Validações dinâmicas
- **Classes:** 
  - `PermissionValidator` - Validar roles
  - `TransitionValidator` - Validar transições (regras em JSON)
  - `EntityTypeValidator` - Validar tipos de entidade
- **Benefício:** Regras sem if/else, configuráveis em JSON

### 3. Command Pattern
- **Objetivo:** Encapsular operações com histórico
- **Classes:** `MoveCardCommand`, `CommandExecutor`, `CommandResult`
- **Benefício:** Histórico de operações, suporte a undo/redo, auditoria

### 4. Observer Pattern
- **Objetivo:** Notificações sem acoplamento
- **Classes:** `WorkflowEventManager`, `WorkflowEventListener`, `CardMovedEvent`
- **Benefício:** Desacoplamento entre componentes

### 5. Circuit Breaker Pattern
- **Objetivo:** Resiliência para chamadas externas
- **Tecnologia:** Quarkus Fault Tolerance (Retry + CircuitBreaker)
- **Benefício:** App não cai se Notification Service falhar

---

## 📊 Validações em Camadas

```
┌─────────────────────────────────────────┐
│ 1. ENTRADA                              │
│ - JWT válido?                           │
│ - Card/Column existem?                  │
│ - Parâmetros não vazios?                │
└─────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────┐
│ 2. AUTORIZAÇÃO (Roles)                  │
│ - User tem role para canMoveOut?        │
│ - User tem role para canMoveIn?         │
└─────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────┐
│ 3. NEGÓCIO                              │
│ - Transição configurada? (JSON)         │
│ - Entity type permitido? (JSON)         │
└─────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────┐
│ 4. EXECUÇÃO                             │
│ - Update database                       │
│ - Salvar em histórico                   │
└─────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────┐
│ 5. NOTIFICAÇÃO                          │
│ - Disparar evento                       │
│ - Chamar Notification Service           │
│ - Registrar auditoria                   │
└─────────────────────────────────────────┘
```

---

## 🔐 Entidades JPA

### Pipeline
```
id              UUID
name            String
context_type    String (ex: "edital")
context_id      String (ex: "edital-123")
allowed_roles_view       JSONB
allowed_roles_manage     JSONB
created_at      Timestamp
updated_at      Timestamp
```

### PipelineColumn
```
id                  UUID
pipeline_id         UUID (FK)
key                 String (ex: "inscritos")
name                String (ex: "Inscritos")
position            Integer
allowed_entity_types        JSONB (ex: ["project"])
allowed_roles_view          JSONB
allowed_roles_move_in       JSONB
allowed_roles_move_out      JSONB
transition_rules_json       JSONB
notification_rules_json     JSONB
card_layout_json            JSONB
filter_config_json          JSONB
created_at          Timestamp
updated_at          Timestamp
```

### PipelineCard
```
id              UUID
pipeline_id     UUID (FK)
column_id       UUID (FK)
entity_type     String (ex: "project")
entity_id       String (ex: "proj-1")
sort_order      Integer
data_snapshot_json  JSONB
created_at      Timestamp
updated_at      Timestamp
```

---

## 📝 Exemplo: transition_rules_json

```json
{
  "transitions": [
    {
      "from": "inscritos",
      "to": "em_avaliacao",
      "allowedRoles": ["admin", "gestor"]
    },
    {
      "from": "em_avaliacao",
      "to": "aprovados",
      "allowedRoles": ["admin"]
    }
  ]
}
```

---

## 📝 Exemplo: notification_rules_json

```json
{
  "on_enter": [
    {
      "eventType": "PROJECT_READY_FOR_REVIEW",
      "channels": ["email"],
      "recipients": ["project_owner"]
    }
  ]
}
```

---

## 📝 Exemplo: card_layout_json

```json
{
  "title": "title",
  "subtitle": "ownerName",
  "tags": ["status", "score"]
}
```

---

## ✅ Implementação Atual

- [x] Entidades JPA (Pipeline, Column, Card)
- [x] Repositories (DAO pattern)
- [x] Validadores (Strategies)
- [x] Comando de movimento (Command Pattern)
- [x] Event Manager (Observer Pattern)
- [x] Serviço de workflow
- [x] Controller REST
- [ ] NotificationDispatcher (integração com Notification Service)
- [ ] AuditEventDispatcher (registro de auditoria)
- [ ] Testes unitários
- [ ] Testes de integração

---

## 🚀 Próximas Etapas

1. **Implementar NotificationDispatcher** - Integração com Notification Service
2. **Implementar AuditEventDispatcher** - Registro de operações
3. **Criar Mappers** - Entity ↔ DTO
4. **Adicionar Testes** - Unitários e integração
5. **Configurar CI/CD** - Build e deploy
6. **Documentação API** - OpenAPI/Swagger

---

## 📚 Tecnologias

- **Framework:** Quarkus
- **ORM:** Hibernate + Panache
- **Banco:** PostgreSQL (Supabase)
- **Padrões:** Repository, Strategy, Command, Observer, Circuit Breaker
- **Validação:** Custom validators + Quarkus Fault Tolerance
