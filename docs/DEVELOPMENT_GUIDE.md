# 📋 Guia de Desenvolvimento - Workflow Service

## 1️⃣ Padrões de Código

### 1.1 Convenções de Nomenclatura

```java
// ✅ BOM: Nomes descritivos e claros
public List<PipelineCard> findByColumnId(Long columnId) { }
public boolean canMoveOut(UserContext user, List<String> allowedRoles) { }
private void validateInput() { }

// ❌ RUIM: Nomes genéricos ou abreviados
public List<PipelineCard> find(Long id) { }
public boolean can(UserContext u, List<String> ar) { }
private void validate() { }
```

### 1.2 Organização de Imports

```java
// ✅ Ordem esperada:
// 1. Imports do Java (java.*, javax.*)
// 2. Imports de bibliotecas externas (io.quarkus.*, jakarta.*)
// 3. Imports do projeto (com.apporte.*)

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import com.apporte.domain.model.Pipeline;
```

### 1.3 Logging

```java
// ✅ BOM: Usar parâmetros do Log
Log.infof("Card %d movido para coluna %s por usuário %s", cardId, columnId, userId);
Log.warnf("Pipeline %d não encontrado", pipelineId);
Log.errorf("Erro ao salvar: %s", exception.getMessage());

// ❌ RUIM: Concatenação de strings
Log.info("Card " + cardId + " moved to column " + columnId);
```

### 1.4 Null Safety

```java
// ✅ BOM: Usar Objects.requireNonNull
import java.util.Objects;
Objects.requireNonNull(user, "User context não pode ser null");

// ✅ BOM: Validar antes de usar
if (card == null) {
    throw new IllegalArgumentException("Card não encontrado");
}
```

### 1.5 Validações em DTOs

```java
// ✅ BOM: Usar annotations do Jakarta Validation
import jakarta.validation.constraints.*;

public class MoveCardRequest {
    @NotBlank(message = "fromColumnId não pode ser vazio")
    private String fromColumnId;
    
    @NotNull(message = "toColumnId é obrigatório")
    private String toColumnId;
}

// ❌ RUIM: Sem validações
public class MoveCardRequest {
    private String fromColumnId;
    private String toColumnId;
}
```

### 1.6 Documentação JavaDoc

```java
// ✅ BOM: Documentar classe, métodos públicos e exceções
/**
 * Valida permissões de um usuário baseado em roles.
 * Implementa Strategy Pattern para permitir diferentes estratégias de validação.
 */
@ApplicationScoped
public class RoleBasedPermissionValidator implements PermissionValidator {
    
    /**
     * Verifica se o usuário pode mover cards para fora da coluna.
     *
     * @param user Contexto do usuário autenticado
     * @param allowedRoles Lista de roles permitidas (null = sem restrição)
     * @return true se tem permissão, false caso contrário
     */
    @Override
    public boolean canMoveOut(UserContext user, List<String> allowedRoles) {
        // ...
    }
}
```

---

## 2️⃣ Test-Driven Development (TDD)

### 2.1 Estrutura de Testes

```
src/test/java/com/apporte/
├── validator/
│   ├── PermissionValidatorTest.java
│   ├── TransitionValidatorTest.java
│   └── EntityTypeValidatorTest.java
├── service/
│   └── WorkflowServiceTest.java
├── command/
│   └── MoveCardCommandTest.java
└── repository/
    └── PipelineCardRepositoryTest.java
```

### 2.2 Padrão Red-Green-Refactor

**1️⃣ RED: Escrever teste que falha**
```java
@Test
void testCanMoveOutWithValidRole() {
    // Arrange
    UserContext user = new UserContext(1L, "João", List.of("APPROVER"));
    List<String> allowedRoles = List.of("APPROVER", "ADMIN");
    
    // Act
    boolean result = validator.canMoveOut(user, allowedRoles);
    
    // Assert
    assertTrue(result);
}
```

**2️⃣ GREEN: Implementar código mínimo para passar**
```java
@Override
public boolean canMoveOut(UserContext user, List<String> allowedRoles) {
    return user.getRoles().stream()
        .anyMatch(allowedRoles::contains);
}
```

**3️⃣ REFACTOR: Melhorar código mantendo testes passando**
```java
@Override
public boolean canMoveOut(UserContext user, List<String> allowedRoles) {
    Objects.requireNonNull(user, "User não pode ser null");
    
    if (allowedRoles == null || allowedRoles.isEmpty()) {
        return true; // Sem restrição
    }
    
    return user.getRoles().stream()
        .anyMatch(allowedRoles::contains);
}
```

### 2.3 Template de Teste Unitário

```java
@QuarkusTest
class PermissionValidatorTest {
    
    @Inject
    private PermissionValidator validator;
    
    private UserContext user;
    private List<String> allowedRoles;
    
    @BeforeEach
    void setUp() {
        user = new UserContext(1L, "João", List.of("APPROVER"));
        allowedRoles = List.of("APPROVER", "ADMIN");
    }
    
    // Testes de sucesso
    @Test
    void testCanMoveOut_WithValidRole_ShouldReturnTrue() {
        // Arrange
        // já feito em setUp()
        
        // Act
        boolean result = validator.canMoveOut(user, allowedRoles);
        
        // Assert
        assertTrue(result, "Usuário com role válida deve conseguir mover");
    }
    
    // Testes de falha
    @Test
    void testCanMoveOut_WithoutRole_ShouldReturnFalse() {
        // Arrange
        UserContext unauthorizedUser = new UserContext(2L, "Maria", List.of("VIEWER"));
        
        // Act
        boolean result = validator.canMoveOut(unauthorizedUser, allowedRoles);
        
        // Assert
        assertFalse(result, "Usuário sem role permitida não deve conseguir mover");
    }
    
    // Testes de edge cases
    @Test
    void testCanMoveOut_WithNullRoles_ShouldReturnTrue() {
        // Arrange
        // null roles significa sem restrição
        
        // Act
        boolean result = validator.canMoveOut(user, null);
        
        // Assert
        assertTrue(result, "Sem roles permitidas significa sem restrição");
    }
    
    // Testes de erro
    @Test
    void testCanMoveOut_WithNullUser_ShouldThrowException() {
        // Arrange
        // Act & Assert
        assertThrows(NullPointerException.class, 
            () -> validator.canMoveOut(null, allowedRoles),
            "User context não pode ser null"
        );
    }
}
```

### 2.4 Cobertura de Testes

```bash
# Executar testes com cobertura
./mvnw clean test jacoco:report

# Verificar cobertura em:
# target/site/jacoco/index.html
```

**Metas de Cobertura:**
- ✅ **Validators:** 100% (crítico)
- ✅ **Commands:** 95%+ (crítico)
- ✅ **Service:** 90%+
- ✅ **Repository:** 80%+
- ✅ **DTOs/Models:** 60%+

---

## 3️⃣ Arquitetura em Camadas

### 3.1 Responsabilidades por Camada

```
┌─────────────────────────────────────────┐
│ Controller (REST)                       │
│ - Receber requisições HTTP              │
│ - Extrair JWT                           │
│ - Validar Content-Type                  │
│ - Serializar/Deserializar JSON          │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│ Service (Orquestração)                  │
│ - Chamar repositories                   │
│ - Chamar commands                       │
│ - Chamar validators                     │
│ - Disparar eventos                      │
│ - Transações (@Transactional)           │
└──────────────────┬──────────────────────┘
                   │
       ┌───────────┼───────────┐
       │           │           │
┌──────▼──┐ ┌──────▼──┐ ┌──────▼───────┐
│Command  │ │Validator│ │Repository    │
│Pattern  │ │Strategy │ │(Data Access) │
└────────┘ └────────┘ └──────────────┘
```

### 3.2 Fluxo de Requisição

```
1. HTTP POST /api/pipelines/1/cards/5/move
   ↓
2. WorkflowController
   ├─ Extrair JWT → UserContext
   ├─ Validar @RequestBody
   ├─ Chamar WorkflowService
   └─ Serializar resposta
   ↓
3. WorkflowService
   ├─ Buscar card (Repository)
   ├─ Criar MoveCardCommand
   ├─ Executar command
   ├─ Disparar evento
   └─ Retornar MoveCardResponse
   ↓
4. MoveCardCommand.execute()
   ├─ PermissionValidator.canMoveOut() ✓
   ├─ PermissionValidator.canMoveIn() ✓
   ├─ TransitionValidator.isValid() ✓
   ├─ EntityTypeValidator.isValid() ✓
   └─ cardRepository.persist()
   ↓
5. HTTP 200 OK + JSON
```

---

## 4️⃣ Padrões de Projeto

### 4.1 Repository Pattern

```java
// Interface abstrai o acesso aos dados
public interface PanacheRepository<PipelineCard> {
    PipelineCard findById(Long id);
    void persist(PipelineCard entity);
}

// Implementação: uso de Panache
@ApplicationScoped
public class PipelineCardRepository implements PanacheRepository<PipelineCard> {
    public List<PipelineCard> findByColumnId(Long columnId) {
        return find("columnId = ?1 ORDER BY sortOrder ASC", columnId).list();
    }
}

// Uso no Service
@Inject
private PipelineCardRepository cardRepository;

public PipelineCard getCard(Long id) {
    return cardRepository.findById(id);
}
```

### 4.2 Strategy Pattern (Validators)

```java
// Interface define o contrato
public interface PermissionValidator {
    boolean canMoveOut(UserContext user, List<String> allowedRoles);
}

// Implementações intercambiáveis
@ApplicationScoped
public class RoleBasedPermissionValidator implements PermissionValidator {
    // ...
}

// Possível implementação alternativa no futuro
public class LdapPermissionValidator implements PermissionValidator {
    // Validação contra LDAP
}

// Uso
public MoveCardCommand(/* ... */) {
    this.permissionValidator = new RoleBasedPermissionValidator();
}
```

### 4.3 Command Pattern

```java
// Interface do comando
public interface WorkflowCommand {
    void execute() throws Exception;
}

// Implementação
public class MoveCardCommand implements WorkflowCommand {
    private PipelineCard card;
    private Long fromColumnId;
    private Long toColumnId;
    
    @Override
    public void execute() {
        // Validações
        // Execução
        // Persistência
    }
}

// Executor com histórico
@ApplicationScoped
public class CommandExecutor {
    private List<CommandResult> history = new ArrayList<>();
    
    public CommandResult execute(WorkflowCommand command) {
        try {
            command.execute();
            CommandResult result = new CommandResult(true, "Sucesso");
            history.add(result);
            return result;
        } catch (Exception e) {
            CommandResult result = new CommandResult(false, e.getMessage());
            history.add(result);
            return result;
        }
    }
}
```

### 4.4 Observer Pattern

```java
// Evento
public class CardMovedEvent {
    private String cardId;
    private String fromColumnId;
    private String toColumnId;
    // ...
}

// Listener
public interface WorkflowEventListener {
    void onCardMoved(CardMovedEvent event);
}

// Manager
@ApplicationScoped
public class WorkflowEventManager {
    private List<WorkflowEventListener> listeners = new CopyOnWriteArrayList<>();
    
    public void fireCardMoved(CardMovedEvent event) {
        listeners.forEach(l -> l.onCardMoved(event));
    }
}

// Uso
eventManager.subscribe(new NotificationDispatcher());
eventManager.subscribe(new AuditEventDispatcher());
eventManager.fireCardMoved(event);
```

---

## 5️⃣ Validações em Cascata

### 5.1 Níveis de Validação

```
Nível 1: INPUT
├─ Parâmetros obrigatórios presentes?
├─ Tipos de dados corretos?
└─ IDs válidos?

Nível 2: AUTHORIZATION
├─ JWT válido?
├─ User tem roles?
└─ Permissões específicas?

Nível 3: BUSINESS RULES
├─ Transição configurada no JSON?
├─ Entity type permitido?
└─ Regras de negócio atendidas?

Nível 4: EXECUTION
├─ Database atualizado com sucesso?
└─ Histórico registrado?

Nível 5: NOTIFICATION
├─ Eventos disparados?
└─ Serviços externos notificados?
```

### 5.2 Implementação

```java
@Override
public void execute() {
    try {
        // 1. Validação de entrada
        validateInput();
        
        // 2. Validação de autorização
        if (!permissionValidator.canMoveOut(userContext, fromColumn.allowedRolesMoveOut)) {
            throw new UnauthorizedException("Sem permissão para sair da coluna");
        }
        
        // 3. Validação de negócio
        if (!transitionValidator.isValid(fromColumn.key, toColumn.key)) {
            throw new InvalidTransitionException("Transição não permitida");
        }
        
        // 4. Execução
        card.columnId = toColumnId;
        cardRepository.persist(card);
        
        // 5. Resultado
        result = new CommandResult(true, "Card movido com sucesso");
    } catch (Exception e) {
        result = new CommandResult(false, e.getMessage());
    }
}
```

---

## 6️⃣ Tratamento de Exceções

### 6.1 Exceções Customizadas

```java
// ✅ BOM: Usar exceções específicas
public class UnauthorizedException extends WorkflowException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

public class InvalidTransitionException extends WorkflowException {
    public InvalidTransitionException(String message) {
        super(message);
    }
}

// Uso
if (!hasPermission) {
    throw new UnauthorizedException("Usuário não tem permissão");
}

// ❌ RUIM: Exceções genéricas
throw new RuntimeException("User cannot move");
```

### 6.2 Tratamento no Controller

```java
@ExceptionHandler(UnauthorizedException.class)
public Response handleUnauthorized(UnauthorizedException e) {
    return Response.status(Response.Status.FORBIDDEN)
        .entity(new ErrorResponse(e.getMessage()))
        .build();
}

@ExceptionHandler(InvalidTransitionException.class)
public Response handleInvalidTransition(InvalidTransitionException e) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(new ErrorResponse(e.getMessage()))
        .build();
}
```

---

## 7️⃣ Checklist de Qualidade

Antes de fazer commit:

- [ ] Código segue convenções de nomenclatura
- [ ] JavaDoc adicionado para classes e métodos públicos
- [ ] Logging usando parâmetros (sem concatenação)
- [ ] Validações em todos os DTOs (@NotNull, @NotBlank)
- [ ] Null safety verificado (Objects.requireNonNull)
- [ ] Testes unitários escrito (Red-Green-Refactor)
- [ ] Cobertura de testes >= 80%
- [ ] Sem imports desnecessários
- [ ] Sem código duplicado
- [ ] Exceções customizadas usadas corretamente
- [ ] Transações (@Transactional) onde apropriado
- [ ] Logs em nível apropriado (INFO, WARN, ERROR, DEBUG)
- [ ] Code review feito
- [ ] Testes passando localmente

---

## 8️⃣ Comandos Úteis

```bash
# Executar testes
./mvnw clean test

# Executar testes com cobertura
./mvnw clean test jacoco:report

# Rodar em modo watch
./mvnw quarkus:dev

# Build para produção
./mvnw clean package

# Build nativo (GraalVM)
./mvnw clean package -Pnative

# Formatar código
./mvnw fmt:format

# Verificar qualidade (SonarQube)
./mvnw clean verify sonar:sonar
```

---

## 9️⃣ Recursos

- 📖 [Quarkus Documentation](https://quarkus.io/guides/)
- 📖 [Jakarta EE Specification](https://jakarta.ee/)
- 📖 [Design Patterns](https://refactoring.guru/design-patterns)
- 📖 [Clean Code Principles](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)
- 📖 [Test-Driven Development](https://en.wikipedia.org/wiki/Test-driven_development)

---

**Última atualização:** 23 de dezembro de 2025  
**Status:** Ativo - Guia de desenvolvimento oficial  
**Manutenção:** Deve ser atualizado conforme evoluções arquiteturais
