package com.apporte.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Contexto do usuário para o domínio de negócio.
 * Mantido para compatibilidade com código existente.
 * 
 * <p>Pode ser construído a partir do KeycloakUserContext ou usado standalone.
 * 
 * @since 2.0
 */
public record UserContext(
    String id,
    String email,
    String name,
    String organizationId,
    String organizationName,
    Set<String> roles
) {
    /**
     * Compact constructor with validation.
     */
    public UserContext {
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        roles = roles != null ? Set.copyOf(roles) : Set.of();
    }
    
    /**
     * Creates UserContext from KeycloakUserContext.
     * 
     * @param keycloakContext the Keycloak user context
     * @return a new UserContext instance
     */
    public static UserContext fromKeycloak(com.apporte.security.KeycloakUserContext keycloakContext) {
        return new UserContext(
            keycloakContext.getUserId(),
            keycloakContext.getEmail(),
            keycloakContext.getFullName(),
            keycloakContext.getOrganizationId().orElse(null),
            keycloakContext.getOrganizationName().orElse(null),
            keycloakContext.getRoles()
        );
    }
    
    /**
     * Gets organization ID as Optional.
     */
    public Optional<String> organization() {
        return Optional.ofNullable(organizationId);
    }
    
    /**
     * Checks if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Checks if user has any of the specified roles.
     */
    public boolean hasAnyRole(String... roleNames) {
        return roles.stream().anyMatch(Set.of(roleNames)::contains);
    }
    
    /**
     * Checks if user belongs to an organization.
     */
    public boolean hasOrganization() {
        return organizationId != null && !organizationId.isBlank();
    }
    
    /**
     * Checks if user is a system admin.
     */
    public boolean isSystemAdmin() {
        return hasRole("system-admin");
    }
    
    /**
     * Checks if user is an organization admin.
     */
    public boolean isOrgAdmin() {
        return hasRole("org-admin");
    }
    
    // Legacy compatibility - records auto-generate getId(), getEmail(), getName(), getRoles()
}
