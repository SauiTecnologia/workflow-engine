package com.apporte.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para requisição de movimento de card.
 * 
 * @param fromColumnId ID da coluna de origem
 * @param toColumnId ID da coluna de destino
 * @since 2.0
 */
public record MoveCardRequest(
    @NotBlank(message = "fromColumnId não pode ser vazio")
    String fromColumnId,
    
    @NotBlank(message = "toColumnId não pode ser vazio")
    String toColumnId
) {}
