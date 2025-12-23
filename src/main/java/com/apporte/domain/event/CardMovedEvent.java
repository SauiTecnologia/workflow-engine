package com.apporte.domain.event;

import com.apporte.domain.model.UserContext;
import java.time.LocalDateTime;

/**
 * Evento disparado quando um card é movido entre colunas
 */
public class CardMovedEvent {
    private String cardId;
    private String pipelineId;
    private String fromColumnId;
    private String toColumnId;
    private String entityType;
    private String entityId;
    private UserContext userContext;
    private LocalDateTime timestamp;

    public CardMovedEvent() {
    }

    public CardMovedEvent(String cardId, String pipelineId, String fromColumnId, 
                         String toColumnId, String entityType, String entityId, 
                         UserContext userContext) {
        this.cardId = cardId;
        this.pipelineId = pipelineId;
        this.fromColumnId = fromColumnId;
        this.toColumnId = toColumnId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userContext = userContext;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getPipelineId() { return pipelineId; }
    public void setPipelineId(String pipelineId) { this.pipelineId = pipelineId; }

    public String getFromColumnId() { return fromColumnId; }
    public void setFromColumnId(String fromColumnId) { this.fromColumnId = fromColumnId; }

    public String getToColumnId() { return toColumnId; }
    public void setToColumnId(String toColumnId) { this.toColumnId = toColumnId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public UserContext getUserContext() { return userContext; }
    public void setUserContext(UserContext userContext) { this.userContext = userContext; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "CardMovedEvent{" +
                "cardId='" + cardId + '\'' +
                ", pipelineId='" + pipelineId + '\'' +
                ", fromColumnId='" + fromColumnId + '\'' +
                ", toColumnId='" + toColumnId + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
