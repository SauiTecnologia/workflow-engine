package com.apporte.repository;

import com.apporte.domain.model.PipelineCard;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Repository para PipelineCard.
 * Implementa o Repository Pattern para acesso a dados de cards.
 */
@ApplicationScoped
public class PipelineCardRepository implements PanacheRepository<PipelineCard> {
    
    /**
     * Busca todos os cards de uma coluna, ordenados por sortOrder.
     *
     * @param columnId ID da coluna
     * @return Lista de cards ordenados
     */
    public List<PipelineCard> findByColumnId(Long columnId) {
        return find("columnId = ?1 ORDER BY sortOrder ASC", columnId).list();
    }
    
    /**
     * Busca um card por ID.
     *
     * @param cardId ID do card
     * @return Card encontrado ou vazio
     */
    public Optional<PipelineCard> findByIdOptional(Long cardId) {
        return findByIdOptional(cardId);
    }
    
    /**
     * Busca cards de um pipeline.
     *
     * @param pipelineId ID do pipeline
     * @return Lista de cards do pipeline
     */
    public List<PipelineCard> findByPipelineId(Long pipelineId) {
        return find("pipelineId = ?1", pipelineId).list();
    }
    
    /**
     * Busca um card por entityType e entityId.
     * Útil para encontrar um card representando uma entidade específica.
     *
     * @param entityType Tipo da entidade (ex: "project")
     * @param entityId ID da entidade
     * @return Card encontrado ou null
     */
    public PipelineCard findByEntity(String entityType, String entityId) {
        return find("entityType = ?1 and entityId = ?2", entityType, entityId)
            .firstResult();
    }
}