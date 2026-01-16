package com.apporte.validator;

import com.apporte.domain.model.UserContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;

/**
 * Implementação de PermissionValidator baseada em roles.
 * Valida permissões comparando as roles do usuário com as roles permitidas
 * configuradas nas colunas do pipeline.
 * 
 * Implementa Strategy Pattern para permitir trocar a estratégia de validação.
 */
@ApplicationScoped
public class RoleBasedPermissionValidator implements PermissionValidator {
    
    @Override
    public boolean canViewPipeline(UserContext user, List<String> allowedRoles) {
        return canPerformAction(user, allowedRoles);
    }
    
    @Override
    public boolean canMoveOut(UserContext user, List<String> allowedRoles) {
        return canPerformAction(user, allowedRoles);
    }
    
    @Override
    public boolean canMoveIn(UserContext user, List<String> allowedRoles) {
        return canPerformAction(user, allowedRoles);
    }
    
    /**
     * Valida se o usuário tem pelo menos uma das roles permitidas.
     * Se allowedRoles é null ou vazio, retorna true (sem restrição).
     *
     * @param user Contexto do usuário
     * @param allowedRoles Lista de roles permitidas
     * @return true se o usuário tem permissão
     */
    private boolean canPerformAction(UserContext user, List<String> allowedRoles) {
        Objects.requireNonNull(user, "User context não pode ser null");
        
        // Sem restrição de roles
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true;
        }
        
        // Verificar se ao menos uma role do usuário está na lista permitida
        return user.roles().stream()
            .anyMatch(allowedRoles::contains);
    }
}
