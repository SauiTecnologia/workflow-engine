package com.apporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para requisição de movimento de card.
 * Valida que os IDs das colunas são fornecidos e não são vazios.
 */
public class MoveCardRequest {
    
    @NotBlank(message = "fromColumnId não pode ser vazio")
    private String fromColumnId;
    
    @NotBlank(message = "toColumnId não pode ser vazio")
    private String toColumnId;

    /**
     * Construtor padrão para deserialização JSON.
     */
    public MoveCardRequest() {
    }

    /**
     * Construtor com parâmetros.
     *
     * @param fromColumnId ID da coluna de origem
     * @param toColumnId ID da coluna de destino
     */
    public MoveCardRequest(String fromColumnId, String toColumnId) {
        this.fromColumnId = fromColumnId;
        this.toColumnId = toColumnId;
    }

    public String getFromColumnId() {
        return fromColumnId;
    }

    public void setFromColumnId(String fromColumnId) {
        this.fromColumnId = fromColumnId;
    }

    public String getToColumnId() {
        return toColumnId;
    }

    public void setToColumnId(String toColumnId) {
        this.toColumnId = toColumnId;
    }

    @Override
    public String toString() {
        return "MoveCardRequest{" +
                "fromColumnId='" + fromColumnId + '\'' +
                ", toColumnId='" + toColumnId + '\'' +
                '}';
    }
}
