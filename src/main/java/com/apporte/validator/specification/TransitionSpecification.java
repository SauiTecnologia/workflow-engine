package com.apporte.validator.specification;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;

/**
 * Especificação para validação de transições entre colunas.
 * Implementa o padrão Specification para lógica de validação reutilizável.
 */
@ApplicationScoped
public class TransitionSpecification {
    
    /**
     * Valida se uma transição entre colunas é permitida.
     *
     * @param fromColumnId ID da coluna origem
     * @param toColumnId ID da coluna destino
     * @return true se a transição é permitida
     */
    public boolean isValidTransition(Long fromColumnId, Long toColumnId) {
        Objects.requireNonNull(fromColumnId, "fromColumnId não pode ser null");
        Objects.requireNonNull(toColumnId, "toColumnId não pode ser null");
        
        // Não permite mover para a mesma coluna
        if (fromColumnId.equals(toColumnId)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Valida se uma coluna ID é válida.
     *
     * @param columnId ID da coluna
     * @return true se o ID é válido
     */
    public boolean isValidColumnId(Long columnId) {
        return columnId != null && columnId > 0;
    }
    
    /**
     * Valida se um card ID é válido.
     *
     * @param cardId ID do card
     * @return true se o ID é válido
     */
    public boolean isValidCardId(Long cardId) {
        return cardId != null && cardId > 0;
    }
    
    /**
     * Valida se um pipeline ID é válido.
     *
     * @param pipelineId ID do pipeline
     * @return true se o ID é válido
     */
    public boolean isValidPipelineId(Long pipelineId) {
        return pipelineId != null && pipelineId > 0;
    }
}
