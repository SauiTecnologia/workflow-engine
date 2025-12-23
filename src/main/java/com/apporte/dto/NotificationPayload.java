package com.apporte.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Payload de notificação para enviar ao Notification Service.
 * Encapsula informações sobre eventos de workflow que requerem notificação.
 */
public class NotificationPayload {
    private String eventType;
    private String entityType;
    private String entityId;
    private List<String> channels;
    private List<String> recipients;
    private Map<String, Object> context;
    private LocalDateTime timestamp;

    /**
     * Construtor padrão.
     */
    public NotificationPayload() {
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
    }

    /**
     * Construtor com parâmetros principais.
     */
    public NotificationPayload(
        String eventType,
        String entityType,
        String entityId,
        List<String> channels,
        List<String> recipients
    ) {
        this.eventType = Objects.requireNonNull(eventType, "eventType não pode ser null");
        this.entityType = Objects.requireNonNull(entityType, "entityType não pode ser null");
        this.entityId = Objects.requireNonNull(entityId, "entityId não pode ser null");
        this.channels = Objects.requireNonNull(channels, "channels não pode ser null");
        this.recipients = Objects.requireNonNull(recipients, "recipients não pode ser null");
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Adiciona um valor ao contexto da notificação.
     *
     * @param key Chave do contexto
     * @param value Valor associado
     */
    public void addContextValue(String key, Object value) {
        if (this.context == null) {
            this.context = new HashMap<>();
        }
        this.context.put(key, value);
    }

    @Override
    public String toString() {
        return "NotificationPayload{" +
                "eventType='" + eventType + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", channels=" + channels +
                ", recipients=" + recipients +
                ", timestamp=" + timestamp +
                '}';
    }
}
