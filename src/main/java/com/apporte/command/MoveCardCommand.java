package com.apporte.command;

import com.apporte.domain.exception.*;
import com.apporte.domain.model.PipelineCard;
import com.apporte.domain.model.PipelineColumn;
import com.apporte.domain.model.UserContext;
import com.apporte.repository.PipelineCardRepository;
import com.apporte.repository.PipelineColumnRepository;
import com.apporte.validator.EntityTypeValidator;
import com.apporte.validator.PermissionValidator;
import com.apporte.validator.TransitionValidator;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;

/**
 * Comando para mover um card de uma coluna para outra
 * Implementa todas as validações necessárias
 */
public class MoveCardCommand implements WorkflowCommand {
    
    private PipelineCard card;
    private Long fromColumnId;
    private Long toColumnId;
    private UserContext userContext;
    private CommandResult result;
    
    @Inject
    private PipelineCardRepository cardRepository;
    
    @Inject
    private PipelineColumnRepository columnRepository;
    
    @Inject
    private PermissionValidator permissionValidator;
    
    @Inject
    private TransitionValidator transitionValidator;
    
    @Inject
    private EntityTypeValidator entityTypeValidator;
    
    public MoveCardCommand(
        PipelineCard card,
        Long fromColumnId,
        Long toColumnId,
        UserContext userContext
    ) {
        this.card = card;
        this.fromColumnId = fromColumnId;
        this.toColumnId = toColumnId;
        this.userContext = userContext;
    }
    
    @Override
    public void execute() {
        try {
            // 1. Validações
            validateInput();
            
            PipelineColumn fromColumn = columnRepository.findById(fromColumnId);
            PipelineColumn toColumn = columnRepository.findById(toColumnId);
            
            if (fromColumn == null) {
                throw new InvalidInputException("From column not found: " + fromColumnId);
            }
            if (toColumn == null) {
                throw new InvalidInputException("To column not found: " + toColumnId);
            }
            
            // 2. Validação de permissão (sair da coluna)
            if (!permissionValidator.canMoveOut(userContext, fromColumn.allowedRolesMoveOut)) {
                throw new UnauthorizedException(
                    "User cannot move cards out of column: " + fromColumn.key
                );
            }
            
            // 3. Validação de permissão (entrar na coluna)
            if (!permissionValidator.canMoveIn(userContext, toColumn.allowedRolesMoveIn)) {
                throw new UnauthorizedException(
                    "User cannot move cards into column: " + toColumn.key
                );
            }
            
            // 4. Validação de transição
            transitionValidator.validateTransition(
                fromColumn.key,
                toColumn.key,
                userContext,
                fromColumn.transitionRulesJson
            );
            
            // 5. Validação de tipo de entidade
            entityTypeValidator.validateEntityType(
                card.entityType,
                toColumn.allowedEntityTypes
            );
            
            // 6. Executar movimento
            card.columnId = toColumnId;
            cardRepository.persist(card);
            
            result = new CommandResult(
                true,
                "Card moved successfully from column " + fromColumn.key + 
                " to " + toColumn.key
            );
            
            Log.info("Card " + card.id + " moved from column " + fromColumnId + 
                    " to " + toColumnId + " by user " + userContext.id());
            
        } catch (WorkflowException e) {
            result = new CommandResult(false, e.getMessage());
            Log.warn("Workflow exception during card move: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            result = new CommandResult(false, "Unexpected error: " + e.getMessage());
            Log.error("Unexpected error during card move", e);
            throw new WorkflowException("Error moving card") {
            };
        }
    }
    
    @Override
    public void undo() {
        // Restaurar para coluna anterior
        card.columnId = fromColumnId;
        cardRepository.persist(card);
        Log.info("Card move undone for card: " + card.id);
    }
    
    @Override
    public CommandResult getResult() {
        return result;
    }
    
    private void validateInput() {
        if (card == null) {
            throw new InvalidInputException("Card cannot be null");
        }
        if (card.id == null) {
            throw new InvalidInputException("Card ID cannot be null");
        }
        if (fromColumnId == null) {
            throw new InvalidInputException("From column ID cannot be null");
        }
        if (toColumnId == null) {
            throw new InvalidInputException("To column ID cannot be null");
        }
        if (fromColumnId.equals(toColumnId)) {
            throw new InvalidInputException("From and to columns cannot be the same");
        }
        if (userContext == null) {
            throw new InvalidInputException("User context cannot be null");
        }
    }
}
