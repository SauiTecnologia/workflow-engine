package com.apporte.dto;

import java.time.LocalDateTime;

/**
 * DTO para resposta do movimento de card.
 * Contém informações sobre o resultado do movimento do card.
 */
public class MoveCardResponse {
    private Long cardId;
    private String newColumnId;
    private boolean success;
    private String message;
    private LocalDateTime timestamp;

    /**
     * Construtor padrão.
     */
    public MoveCardResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Construtor com parâmetros.
     */
    public MoveCardResponse(Long cardId, String newColumnId, boolean success, String message) {
        this.cardId = cardId;
        this.newColumnId = newColumnId;
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getNewColumnId() {
        return newColumnId;
    }

    public void setNewColumnId(String newColumnId) {
        this.newColumnId = newColumnId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MoveCardResponse{" +
                "cardId=" + cardId +
                ", newColumnId='" + newColumnId + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
