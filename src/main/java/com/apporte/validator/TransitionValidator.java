package com.apporte.validator;

import com.apporte.domain.exception.InvalidTransitionException;
import com.apporte.domain.model.UserContext;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

/**
 * Valida se transição entre colunas é permitida
 * Implementa Strategy Pattern
 */
public interface TransitionValidator {
    
    /**
     * Verifica se transição é permitida
     * @throws InvalidTransitionException se não permitida
     */
    void validateTransition(
        String fromColumnKey,
        String toColumnKey,
        UserContext user,
        Map<String, Object> transitionRulesJson
    );
}

/**
 * Implementação configurável com regras em JSON
 */
@ApplicationScoped
class ConfigurableTransitionValidator implements TransitionValidator {
    
    @Override
    public void validateTransition(
        String fromColumnKey,
        String toColumnKey,
        UserContext user,
        Map<String, Object> transitionRulesJson
    ) {
        // Se não há regras, permitir transição
        if (transitionRulesJson == null || transitionRulesJson.isEmpty()) {
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transitions = 
                (List<Map<String, Object>>) transitionRulesJson.get("transitions");
            
            if (transitions == null || transitions.isEmpty()) {
                return;
            }
            
            // Procurar pela transição configurada
            var transitionConfig = transitions.stream()
                .filter(t -> fromColumnKey.equals(t.get("from")) &&
                            toColumnKey.equals(t.get("to")))
                .findFirst();
            
            if (transitionConfig.isEmpty()) {
                throw new InvalidTransitionException(
                    String.format(
                        "Transition from '%s' to '%s' is not configured",
                        fromColumnKey,
                        toColumnKey
                    )
                );
            }
            
            // Validar se usuário tem role permitida para essa transição
            @SuppressWarnings("unchecked")
            List<String> allowedRoles = 
                (List<String>) transitionConfig.get().get("allowedRoles");
            
            if (allowedRoles != null && !allowedRoles.isEmpty()) {
                if (!user.hasAnyRole(allowedRoles.toArray(new String[0]))) {
                    throw new InvalidTransitionException(
                        String.format(
                            "User does not have permission for transition '%s' -> '%s'. Required roles: %s",
                            fromColumnKey,
                            toColumnKey,
                            allowedRoles
                        )
                    );
                }
            }
            
        } catch (ClassCastException e) {
            Log.error("Invalid transition rules format", e);
            throw new InvalidTransitionException("Invalid transition rules format", e);
        }
    }
}
