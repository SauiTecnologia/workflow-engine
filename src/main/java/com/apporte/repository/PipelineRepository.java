package com.apporte.repository;

import com.apporte.domain.model.Pipeline;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository para Pipeline
 */
@ApplicationScoped
public class PipelineRepository implements PanacheRepository<Pipeline> {
    
    /**
     * Busca pipeline por contextId
     */
    public Pipeline findByContextId(String contextId) {
        return find("contextId", contextId).firstResult();
    }
    
    /**
     * Busca pipeline por contextType e contextId
     */
    public Pipeline findByContextTypeAndId(String contextType, String contextId) {
        return find("contextType = ?1 and contextId = ?2", contextType, contextId)
            .firstResult();
    }
}
