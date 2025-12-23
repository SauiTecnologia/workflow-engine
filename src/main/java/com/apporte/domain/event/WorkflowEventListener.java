package com.apporte.domain.event;

import com.apporte.domain.model.UserContext;

/**
 * Interface para observers de eventos de workflow
 * Implementa Observer Pattern
 */
public interface WorkflowEventListener {
    
    /**
     * Chamado quando um card é movido
     */
    void onCardMoved(CardMovedEvent event);
}
