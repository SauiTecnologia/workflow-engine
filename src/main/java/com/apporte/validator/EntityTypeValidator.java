package com.apporte.validator;

import com.apporte.domain.exception.InvalidEntityTypeException;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Valida se tipo de entidade é permitido em uma coluna
 * Implementa Strategy Pattern
 */
public interface EntityTypeValidator {
    
    /**
     * Verifica se entity type é permitido na coluna
     * @throws InvalidEntityTypeException se não permitido
     */
    void validateEntityType(String entityType, List<String> allowedEntityTypes);
}

/**
 * Implementação configurável
 */
@ApplicationScoped
class ConfigurableEntityTypeValidator implements EntityTypeValidator {
    
    @Override
    public void validateEntityType(String entityType, List<String> allowedEntityTypes) {
        if (allowedEntityTypes == null || allowedEntityTypes.isEmpty()) {
            return; // Sem restrição
        }
        
        if (!allowedEntityTypes.contains(entityType)) {
            throw new InvalidEntityTypeException(
                String.format(
                    "Entity type '%s' not allowed in this column. Allowed types: %s",
                    entityType,
                    allowedEntityTypes
                )
            );
        }
    }
}
