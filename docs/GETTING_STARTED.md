# 🚀 Guia de Execução - Workflow Service

## 📋 Pré-requisitos

- Java 17+ (Quarkus recomenda 17 ou superior)
- Maven 3.8+
- PostgreSQL 12+ ou Supabase
- Git

## 🛠️ Configuração

### 1. Configurar Banco de Dados

Execute o script SQL em seu banco Supabase:
```bash
# Arquivo: src/main/resources/schema.sql
# Copie e execute no Supabase SQL Editor
```

Ou execute via psql:
```bash
psql -h your-host -U postgres -d postgres < src/main/resources/schema.sql
```

### 2. Configurar `application.properties`

Edite `src/main/resources/application.properties`:

```properties
# Banco de Dados
quarkus.datasource.jdbc.url=jdbc:postgresql://your-host:5432/your-database
quarkus.datasource.username=postgres
quarkus.datasource.password=your-password

# Hibernate
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.log.sql=true

# Quarkus
quarkus.application.name=workflow-service
quarkus.application.version=1.0.0

# Logging
quarkus.log.level=INFO
quarkus.log.category."com.apporte".level=DEBUG
```

## ▶️ Executar em Desenvolvimento

### Modo Watch (recompila ao salvar)

```bash
./mvnw quarkus:dev
```

A aplicação estará disponível em: `http://localhost:8080`

## 🧪 Testar Endpoints

### 1. Carregar Pipeline

```bash
curl -X GET http://localhost:8080/api/pipelines/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 2. Mover Card

```bash
curl -X POST http://localhost:8080/api/pipelines/1/cards/1/move \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "fromColumnId": "1",
    "toColumnId": "2"
  }'
```

## 📦 Build para Produção

### Build JAR

```bash
./mvnw clean package
```

Gera: `target/quarkus-app/quarkus-run.jar`

### Build Native (GraalVM)

```bash
./mvnw clean package -Pnative
```

**Requer:** GraalVM 22.0+ e native-image instalado

## 🐳 Docker

### Build Docker Image

```bash
# JVM
docker build -f src/main/docker/Dockerfile.jvm -t workflow-service:latest .

# Native (mais rápido e leve)
docker build -f src/main/docker/Dockerfile.native -t workflow-service:latest .
```

### Executar Container

```bash
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/workflow \
  -e QUARKUS_DATASOURCE_USERNAME=postgres \
  -e QUARKUS_DATASOURCE_PASSWORD=password \
  workflow-service:latest
```

## ✅ Validações Implementadas

Ao mover um card, o serviço valida em cascata:

1. **Entrada** - Card e columns existem?
2. **Autorização** - User tem roles para canMoveOut/canMoveIn?
3. **Negócio** - Transição permitida? Entity type permitido?
4. **Execução** - Atualizar database com sucesso
5. **Notificação** - Disparar eventos para Notification Service

## 📊 Exemplo de Fluxo Completo

### 1. Dados no Banco
```sql
-- Pipeline
SELECT * FROM pipelines WHERE context_id = 'edital-2025-001';

-- Colunas
SELECT * FROM pipeline_columns WHERE pipeline_id = 1;

-- Cards
SELECT * FROM pipeline_cards WHERE pipeline_id = 1;
```

### 2. Requisição HTTP
```http
POST http://localhost:8080/api/pipelines/1/cards/1/move
Authorization: Bearer {jwt}
Content-Type: application/json

{
  "fromColumnId": "1",
  "toColumnId": "2"
}
```

### 3. Fluxo Interno
```
WorkflowController
  ↓ Extrair JWT → UserContext
WorkflowService.moveCard()
  ↓ Buscar card e validar
MoveCardCommand.execute()
  ├─ PermissionValidator.canMoveOut() ✓
  ├─ PermissionValidator.canMoveIn() ✓
  ├─ TransitionValidator.validateTransition() ✓
  ├─ EntityTypeValidator.validateEntityType() ✓
  ├─ CardRepository.persist()
  ↓
WorkflowEventManager.fireCardMoved()
  ├─ NotificationDispatcher.onCardMoved()
  └─ AuditEventDispatcher.onCardMoved()
```

### 4. Resposta
```json
{
  "id": "1",
  "columnId": "2",
  "entityType": "project",
  "entityId": "proj-001",
  "sortOrder": 0
}
```

## 🔍 Debugging

### Logs

Por padrão, `com.apporte.*` está em DEBUG:

```bash
# Ver logs em tempo real
./mvnw quarkus:dev | grep -E "(ERROR|WARN|INFO|DEBUG)"

# Salvar em arquivo
./mvnw quarkus:dev > app.log 2>&1 &
```

### Banco de Dados

Verificar dados em tempo real:

```bash
psql -h localhost -U postgres -d workflow -c "SELECT * FROM pipeline_cards;"
```

### Validação de JWT

O `JwtValidator` atualmente retorna um mock user. Para integrar com Supabase:

```java
// Editar src/main/java/com/apporte/security/JwtValidator.java
// Implementar validação real do JWT
```

## 📈 Performance

### Índices Criados
- `idx_pipelines_context` - Buscar pipeline por contexto
- `idx_pipeline_columns_pipeline_id` - Listar colunas
- `idx_pipeline_cards_pipeline_id` - Listar cards
- `idx_pipeline_cards_column_id` - Cards por coluna
- `idx_pipeline_cards_entity` - Buscar card por entidade

## 🔐 Segurança

### JWT Validation
- Implementado em `JwtValidator`
- Extrai `UserContext` do JWT
- Validações de role em cada operação

### CORS (Se necessário)

Adicionar a `application.properties`:
```properties
quarkus.http.cors=true
quarkus.http.cors.origins=https://seu-frontend.com
quarkus.http.cors.methods=GET,PUT,POST,DELETE,OPTIONS
```

## 🚨 Troubleshooting

### Erro: "Não consegue conectar ao banco"
```bash
# Verificar connection string
echo "jdbc:postgresql://host:5432/database"

# Testar conexão
psql -h host -U user -d database
```

### Erro: "Column not found"
```bash
# Executar schema.sql
psql -h host -U user -d database < src/main/resources/schema.sql
```

### Erro: "Unauthorized"
```bash
# Verificar JWT no header
curl -H "Authorization: Bearer {seu-jwt}" ...

# JWT válido deve ter formato: header.payload.signature
```

## 📝 Logs Esperados

```
INFO  [com.apporte.controller.WorkflowController] Pipeline 1 loaded for user user-123
INFO  [com.apporte.service.WorkflowService] Moving card 1 in pipeline 1
INFO  [com.apporte.command.MoveCardCommand] Card 1 moved from column 1 to column 2 by user user-123
INFO  [com.apporte.domain.event.WorkflowEventManager] Firing CardMovedEvent: CardMovedEvent{...}
```

## 🎯 Next Steps

1. Executar `schema.sql` no Supabase
2. Configurar `application.properties` com credenciais
3. Executar `./mvnw quarkus:dev`
4. Testar endpoints com curl
5. Integrar JWT real do Supabase
6. Implementar NotificationDispatcher
7. Adicionar testes
8. Deploy em produção

## 📞 Suporte

Para dúvidas, consulte:
- **WORKFLOW_SERVICE.md** - Documentação técnica
- **ESTRUTURA.md** - Mapa de arquivos
- **SUMARIO.md** - Resumo executivo

