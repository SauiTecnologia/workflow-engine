package com.apporte.validator;

import com.apporte.domain.model.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para RoleBasedPermissionValidator.
 * Segue padrão TDD com testes de sucesso, falha e edge cases.
 */
@DisplayName("RoleBasedPermissionValidator")
class RoleBasedPermissionValidatorTest {
    
    private PermissionValidator validator;
    private UserContext userWithRole;
    private UserContext userWithoutRole;
    private List<String> allowedRoles;
    
    @BeforeEach
    void setUp() {
        validator = new RoleBasedPermissionValidator();
        userWithRole = new UserContext(
            "1",
            "joao@example.com",
            "João Silva",
            "org-123",
            "Organização A",
            Set.of("APPROVER", "EDITOR")
        );
        userWithoutRole = new UserContext(
            "2",
            "maria@example.com",
            "Maria Santos",
            "org-123",
            "Organização A",
            Set.of("VIEWER")
        );
        allowedRoles = List.of("APPROVER", "ADMIN");
    }
    
    @Test
    @DisplayName("canMoveOut: deve retornar true quando usuário tem role permitida")
    void testCanMoveOut_WithValidRole_ReturnsTrue() {
        boolean result = validator.canMoveOut(userWithRole, allowedRoles);
        assertTrue(result, "Usuário com role APPROVER deve conseguir mover");
    }
    
    @Test
    @DisplayName("canMoveOut: deve retornar false quando usuário não tem role permitida")
    void testCanMoveOut_WithoutValidRole_ReturnsFalse() {
        boolean result = validator.canMoveOut(userWithoutRole, allowedRoles);
        assertFalse(result, "Usuário com role VIEWER não deve conseguir mover");
    }
    
    @Test
    @DisplayName("canMoveOut: deve retornar true quando allowedRoles é null")
    void testCanMoveOut_WithNullRoles_ReturnsTrue() {
        boolean result = validator.canMoveOut(userWithRole, null);
        assertTrue(result, "Sem roles permitidas significa sem restrição");
    }
    
    @Test
    @DisplayName("canMoveOut: deve retornar true quando allowedRoles é vazio")
    void testCanMoveOut_WithEmptyRoles_ReturnsTrue() {
        boolean result = validator.canMoveOut(userWithRole, List.of());
        assertTrue(result, "Lista vazia de roles significa sem restrição");
    }
    
    @Test
    @DisplayName("canMoveOut: deve lançar NullPointerException quando user é null")
    void testCanMoveOut_WithNullUser_ThrowsException() {
        assertThrows(NullPointerException.class, 
            () -> validator.canMoveOut(null, allowedRoles),
            "User context não pode ser null"
        );
    }
    
    @Test
    @DisplayName("canMoveIn: deve retornar true quando usuário tem role permitida")
    void testCanMoveIn_WithValidRole_ReturnsTrue() {
        boolean result = validator.canMoveIn(userWithRole, allowedRoles);
        assertTrue(result, "Usuário com role APPROVER deve conseguir entrar");
    }
    
    @Test
    @DisplayName("canMoveIn: deve retornar false quando usuário não tem role permitida")
    void testCanMoveIn_WithoutValidRole_ReturnsFalse() {
        boolean result = validator.canMoveIn(userWithoutRole, allowedRoles);
        assertFalse(result, "Usuário com role VIEWER não deve conseguir entrar");
    }
    
    @Test
    @DisplayName("canViewPipeline: deve retornar true quando usuário tem role permitida")
    void testCanViewPipeline_WithValidRole_ReturnsTrue() {
        boolean result = validator.canViewPipeline(userWithRole, allowedRoles);
        assertTrue(result, "Usuário com role APPROVER deve conseguir visualizar");
    }
    
    @Test
    @DisplayName("canViewPipeline: deve retornar false quando usuário não tem role permitida")
    void testCanViewPipeline_WithoutValidRole_ReturnsFalse() {
        boolean result = validator.canViewPipeline(userWithoutRole, allowedRoles);
        assertFalse(result, "Usuário com role VIEWER não deve conseguir visualizar");
    }
    
    @Test
    @DisplayName("Edge case: deve reconhecer múltiplas roles do usuário")
    void testMultipleRoles_OneMatchesAllowed_ReturnsTrue() {
        UserContext userWithMultipleRoles = new UserContext(
            "3",
            "multi@example.com",
            "Multi User",
            "org-123",
            "Organização A",
            Set.of("EDITOR", "VIEWER", "APPROVER")
        );
        boolean result = validator.canMoveOut(userWithMultipleRoles, List.of("APPROVER"));
        assertTrue(result, "Deve encontrar APPROVER na lista de roles");
    }
    
    @Test
    @DisplayName("Edge case: deve manejar usuário com lista vazia de roles")
    void testUserWithEmptyRoles_AndAllowedRolesNotEmpty_ReturnsFalse() {
        UserContext userWithoutRoles = new UserContext(
            "5",
            "guest@example.com",
            "Guest User",
            "org-123",
            "Organização A",
            Set.of()
        );
        boolean result = validator.canMoveOut(userWithoutRoles, allowedRoles);
        assertFalse(result, "Usuário sem roles não deve ter permissão");
    }
}
