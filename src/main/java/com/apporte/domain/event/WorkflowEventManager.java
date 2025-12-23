package com.apporte.domain.event;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager para disparar eventos de workflow
 * Implementa Observer Pattern
 */
@ApplicationScoped
public class WorkflowEventManager {
    private List<WorkflowEventListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    Event<CardMovedEvent> cardMovedEvent;

    /**
     * Inscreve um listener
     */
    public void subscribe(WorkflowEventListener listener) {
        listeners.add(listener);
        Log.info("Listener subscribed: " + listener.getClass().getSimpleName());
    }

    /**
     * Desinscreve um listener
     */
    public void unsubscribe(WorkflowEventListener listener) {
        listeners.remove(listener);
        Log.info("Listener unsubscribed: " + listener.getClass().getSimpleName());
    }

    /**
     * Dispara evento de card movido
     */
    public void fireCardMoved(CardMovedEvent event) {
        Log.info("Firing CardMovedEvent: " + event);
        
        // Notificar listeners síncronos
        listeners.forEach(listener -> {
            try {
                listener.onCardMoved(event);
            } catch (Exception e) {
                Log.error("Error in listener: " + listener.getClass().getSimpleName(), e);
            }
        });
        
        // Disparar evento assíncrono via CDI
        cardMovedEvent.fireAsync(event);
    }

    /**
     * Obtém quantidade de listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
