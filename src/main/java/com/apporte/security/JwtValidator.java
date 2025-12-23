package com.apporte.security;

import com.apporte.domain.model.UserContext;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.List;

/**
 * Valida e extrai informações do JWT do Supabase
 * NOTA: Implementação simplificada - em produção integrar com Supabase SDK
 */
@ApplicationScoped
public class JwtValidator {
    
    /**
     * Valida JWT e extrai UserContext
     * NOTA: Esta é uma implementação placeholder
     * Em produção, deve integrar com a biblioteca JWT apropriada
     */
    public UserContext validateAndExtract(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        // Remove "Bearer " prefix if present
        String cleanToken = token.replace("Bearer ", "");
        
        try {
            // TODO: Integrar com Supabase JWT validation
            // Por enquanto, criar um UserContext padrão para testes
            Log.warn("JWT validation is not implemented - using mock user");
            
            return new UserContext(
                "user-123",
                "user@example.com",
                "Example User",
                Arrays.asList("admin", "user")
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
    
    /**
     * Valida apenas se token é válido
     */
    public boolean isValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        try {
            validateAndExtract(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
