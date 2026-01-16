package com.apporte.dto;

import java.time.LocalDateTime;

/**
 * DTO para resposta do movimento de card.
 * 
 * @param cardId ID do card movido
 * @param newColumnId ID da nova coluna
 * @param success indica se a operação foi bem-sucedida
 * @param message mensagem descritiva
 * @param timestamp momento da operação
 * @since 2.0
 */
public record MoveCardResponse(
    Long cardId,
    String newColumnId,
    boolean success,
    String message,
    LocalDateTime timestamp
) {
    /**
     * Construtor auxiliar sem timestamp (usa tempo atual).
     */
    public MoveCardResponse(Long cardId, String newColumnId, boolean success, String message) {
        this(cardId, newColumnId, success, message, LocalDateTime.now());
    }
}
