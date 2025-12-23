package com.apporte.service;

import com.apporte.command.CommandExecutor;
import com.apporte.command.CommandResult;
import com.apporte.command.MoveCardCommand;
import com.apporte.domain.event.CardMovedEvent;
import com.apporte.domain.event.WorkflowEventManager;
import com.apporte.domain.model.Pipeline;
import com.apporte.domain.model.PipelineCard;
import com.apporte.domain.model.PipelineColumn;
import com.apporte.domain.model.UserContext;
import com.apporte.dto.MoveCardRequest;
import com.apporte.dto.MoveCardResponse;
import com.apporte.repository.PipelineCardRepository;
import com.apporte.repository.PipelineColumnRepository;
import com.apporte.repository.PipelineRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Serviço principal de Workflow
 * Responsável pela orquestração de operações do Kanban
 */
@ApplicationScoped
public class WorkflowService {
    
    @Inject
    private PipelineRepository pipelineRepository;
    
    @Inject
    private PipelineColumnRepository columnRepository;
    
    @Inject
    private PipelineCardRepository cardRepository;
    
    @Inject
    private CommandExecutor commandExecutor;
    
    @Inject
    private WorkflowEventManager eventManager;
    
    /**
     * Carrega um pipeline com todas suas colunas e cards.
     *
     * @param pipelineId ID do pipeline
     * @return Pipeline encontrado
     * @throws IllegalArgumentException se o pipeline não existir
     */
    public Pipeline getPipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId);
        if (pipeline == null) {
            Log.warnf("Pipeline não encontrado: %d", pipelineId);
            throw new IllegalArgumentException("Pipeline não encontrado: " + pipelineId);
        }
        Log.debugf("Pipeline carregado: %d", pipelineId);
        return pipeline;
    }
    
    /**
     * Carrega as colunas de um pipeline.
     *
     * @param pipelineId ID do pipeline
     * @return Lista de colunas ordenadas
     */
    public List<PipelineColumn> getPipelineColumns(Long pipelineId) {
        return columnRepository.findByPipelineId(pipelineId);
    }
    
    /**
     * Carrega os cards de uma coluna.
     *
     * @param columnId ID da coluna
     * @return Lista de cards ordenados
     */
    public List<PipelineCard> getColumnCards(Long columnId) {
        return cardRepository.findByColumnId(columnId);
    }
    
    /**
     * Obtém um card específico pelo ID.
     *
     * @param cardId ID do card
     * @return Card encontrado
     */
    public PipelineCard getPipelineCard(Long cardId) {
        PipelineCard card = PipelineCard.findById(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Card não encontrado com ID: " + cardId);
        }
        return card;
    }
    
    /**
     * Move um card de uma coluna para outra com todas as validações.
     * Este método implementa o fluxo completo de validações em cascata.
     *
     * @param pipelineId ID do pipeline
     * @param cardId ID do card
     * @param request Dados da requisição de movimento
     * @param userContext Contexto do usuário autenticado
     * @return Resposta com dados do card movido
     * @throws IllegalArgumentException se validações de entrada falharem
     * @throws RuntimeException se a execução do comando falhar
     */
    @Transactional
    public MoveCardResponse moveCard(
        Long pipelineId,
        Long cardId,
        MoveCardRequest request,
        UserContext userContext
    ) {
        Log.infof("Iniciando movimento do card %d no pipeline %d", cardId, pipelineId);
        
        // 1. Validar que pipeline existe
        Pipeline pipeline = getPipeline(pipelineId);
        
        // 2. Buscar o card
        PipelineCard card = cardRepository.findById(cardId);
        if (card == null) {
            Log.warnf("Card não encontrado: %d", cardId);
            throw new IllegalArgumentException("Card não encontrado: " + cardId);
        }
        
        // 3. Validar que card pertence ao pipeline
        if (!card.pipelineId.equals(pipelineId)) {
            Log.warnf("Card %d não pertence ao pipeline %d", cardId, pipelineId);
            throw new IllegalArgumentException("Card não pertence a este pipeline");
        }
        
        // 4. Executar comando de movimento com todas validações
        MoveCardCommand command = new MoveCardCommand(
            card,
            card.columnId,
            Long.parseLong(request.getToColumnId()),
            userContext
        );
        
        CommandResult result = commandExecutor.execute(command);
        
        if (!result.isSuccess()) {
            Log.errorf("Falha ao mover card: %s", result.getMessage());
            throw new RuntimeException("Falha ao mover card: " + result.getMessage());
        }
        
        // 5. Disparar evento para notificações
        PipelineColumn toColumn = columnRepository.findById(Long.parseLong(request.getToColumnId()));
        eventManager.fireCardMoved(
            new CardMovedEvent(
                card.id.toString(),
                pipelineId.toString(),
                card.columnId.toString(),
                request.getToColumnId(),
                card.entityType,
                card.entityId,
                userContext
            )
        );
        
        Log.infof("Card %d movido com sucesso para coluna %s", cardId, request.getToColumnId());
        
        // 6. Retornar resposta
        return mapToResponse(card);
    }
    
    /**
     * Atualiza uma coluna do pipeline com novos valores.
     *
     * @param pipelineId ID do pipeline
     * @param columnId ID da coluna
     * @param columnUpdates Dados atualizados da coluna
     * @param userContext Contexto do usuário autenticado
     * @return Coluna atualizada
     * @throws IllegalArgumentException se a coluna não existir ou não pertencer ao pipeline
     */
    @Transactional
    public PipelineColumn updateColumn(
        Long pipelineId,
        Long columnId,
        PipelineColumn columnUpdates,
        UserContext userContext
    ) {
        Log.infof("Atualizando coluna %d no pipeline %d", columnId, pipelineId);
        
        // Validar permissão (seria feito em um interceptor/filter real)
        PipelineColumn column = columnRepository.findById(columnId);
        if (column == null) {
            Log.warnf("Coluna não encontrada: %d", columnId);
            throw new IllegalArgumentException("Coluna não encontrada: " + columnId);
        }
        
        if (!column.pipelineId.equals(pipelineId)) {
            Log.warnf("Coluna %d não pertence ao pipeline %d", columnId, pipelineId);
            throw new IllegalArgumentException("Coluna não pertence a este pipeline");
        }
        
        // Atualizar campos
        if (columnUpdates.name != null) {
            column.name = columnUpdates.name;
        }
        if (columnUpdates.position != null) {
            column.position = columnUpdates.position;
        }
        if (columnUpdates.allowedEntityTypes != null) {
            column.allowedEntityTypes = columnUpdates.allowedEntityTypes;
        }
        if (columnUpdates.allowedRolesMoveIn != null) {
            column.allowedRolesMoveIn = columnUpdates.allowedRolesMoveIn;
        }
        if (columnUpdates.allowedRolesMoveOut != null) {
            column.allowedRolesMoveOut = columnUpdates.allowedRolesMoveOut;
        }
        if (columnUpdates.transitionRulesJson != null) {
            column.transitionRulesJson = columnUpdates.transitionRulesJson;
        }
        if (columnUpdates.notificationRulesJson != null) {
            column.notificationRulesJson = columnUpdates.notificationRulesJson;
        }
        if (columnUpdates.cardLayoutJson != null) {
            column.cardLayoutJson = columnUpdates.cardLayoutJson;
        }
        
        columnRepository.persist(column);
        Log.infof("Coluna %d atualizada com sucesso", columnId);
        return column;
    }
    
    /**
     * Mapeia PipelineCard para MoveCardResponse
     */
    private MoveCardResponse mapToResponse(PipelineCard card) {
        return new MoveCardResponse(
            card.id,
            card.columnId.toString(),
            true,
            "Card movido com sucesso"
        );
    }
}
