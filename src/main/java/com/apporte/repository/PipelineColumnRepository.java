package com.apporte.repository;

import com.apporte.domain.model.PipelineColumn;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Repository para PipelineColumn
 */
@ApplicationScoped
public class PipelineColumnRepository implements PanacheRepository<PipelineColumn> {
    
    /**
     * Busca todas as colunas de um pipeline
     */
    public List<PipelineColumn> findByPipelineId(Long pipelineId) {
        // Include ORDER BY in the query string
        return find("pipelineId = ?1 ORDER BY position", pipelineId)
            .list();
    }
    
    /**
     * Busca coluna por pipeline e key
     */
    public PipelineColumn findByPipelineAndKey(Long pipelineId, String key) {
        return find("pipelineId = ?1 and key = ?2", pipelineId, key)
            .firstResult();
    }
}