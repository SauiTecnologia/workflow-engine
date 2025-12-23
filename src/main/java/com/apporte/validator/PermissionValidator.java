package com.apporte.validator;

import com.apporte.domain.model.UserContext;
import java.util.List;

/**
 * Validator que implementa Strategy Pattern para validação de permissões.
 * Define contratos para verificar se um usuário tem permissões específicas
 * para executar ações em cards e pipelines.
 */
public interface PermissionValidator {
    
    /**
     * Verifica se o usuário pode visualizar o pipeline.
     *
     * @param user Contexto do usuário autenticado
     * @param allowedRoles Lista de roles permitidas (null significa sem restrição)
     * @return true se o usuário tem permissão, false caso contrário
     */
    boolean canViewPipeline(UserContext user, List<String> allowedRoles);
    
    /**
     * Verifica se o usuário pode mover cards para fora da coluna (origem).
     *
     * @param user Contexto do usuário autenticado
     * @param allowedRoles Lista de roles permitidas (null significa sem restrição)
     * @return true se o usuário tem permissão, false caso contrário
     */
    boolean canMoveOut(UserContext user, List<String> allowedRoles);
    
    /**
     * Verifica se o usuário pode mover cards para dentro da coluna (destino).
     *
     * @param user Contexto do usuário autenticado
     * @param allowedRoles Lista de roles permitidas (null significa sem restrição)
     * @return true se o usuário tem permissão, false caso contrário
     */
    boolean canMoveIn(UserContext user, List<String> allowedRoles);
}
