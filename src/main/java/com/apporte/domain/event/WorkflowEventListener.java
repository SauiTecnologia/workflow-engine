package com.apporte.domain.event;

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
