package com.apporte.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contexto de autenticação do usuário baseado em Keycloak OIDC.
 * 
 * Extrai informações do JWT token incluindo:
 * - Dados do usuário (ID, email, nome)
 * - Roles/papéis do usuário
 * - Informações da organização (multi-tenant)
 * 
 * @author Apporte Team
 */
@RequestScoped
public class KeycloakUserContext {

    private final SecurityIdentity securityIdentity;
    private final JsonWebToken jwt;

    @Inject
    public KeycloakUserContext(SecurityIdentity securityIdentity, JsonWebToken jwt) {
        this.securityIdentity = securityIdentity;
        this.jwt = jwt;
    }

    /**
     * Retorna o ID único do usuário (Keycloak subject)
     */
    public String getUserId() {
        return jwt.getSubject();
    }

    /**
     * Retorna o email do usuário
     */
    public String getEmail() {
        return jwt.getClaim("email");
    }

    /**
     * Retorna o nome completo do usuário
     */
    public String getFullName() {
        String givenName = jwt.getClaim("given_name");
        String familyName = jwt.getClaim("family_name");
        
        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        }
        return jwt.getClaim("name");
    }

    /**
     * Retorna o primeiro nome
     */
    public String getFirstName() {
        return jwt.getClaim("given_name");
    }

    /**
     * Retorna o sobrenome
     */
    public String getLastName() {
        return jwt.getClaim("family_name");
    }

    /**
     * Retorna o username preferido
     */
    public String getUsername() {
        return jwt.getClaim("preferred_username");
    }

    /**
     * Retorna o ID da organização (multi-tenant)
     */
    public Optional<String> getOrganizationId() {
        String orgId = jwt.getClaim("org_id");
        return Optional.ofNullable(orgId);
    }

    /**
     * Retorna o nome da organização
     */
    public Optional<String> getOrganizationName() {
        String orgName = jwt.getClaim("org_name");
        return Optional.ofNullable(orgName);
    }

    /**
     * Retorna os grupos do usuário (organizações)
     */
    public List<String> getGroups() {
        Object groups = jwt.getClaim("groups");
        if (groups instanceof List) {
            return ((List<?>) groups).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Retorna as roles do usuário
     */
    public Set<String> getRoles() {
        return securityIdentity.getRoles();
    }

    /**
     * Verifica se o usuário tem uma role específica
     */
    public boolean hasRole(String role) {
        return securityIdentity.hasRole(role);
    }

    /**
     * Verifica se o usuário é admin do sistema
     */
    public boolean isSystemAdmin() {
        return hasRole("system-admin");
    }

    /**
     * Verifica se o usuário é admin da organização
     */
    public boolean isOrgAdmin() {
        return hasRole("org-admin");
    }

    /**
     * Verifica se o usuário é proponente
     */
    public boolean isProponente() {
        return hasRole("proponente");
    }

    /**
     * Verifica se o usuário é parecerista
     */
    public boolean isParecerista() {
        return hasRole("parecerista");
    }

    /**
     * Verifica se o usuário é investidor
     */
    public boolean isInvestidor() {
        return hasRole("investidor");
    }

    /**
     * Verifica se o usuário pertence a uma organização específica
     */
    public boolean belongsToOrganization(String organizationId) {
        return getOrganizationId()
                .map(orgId -> orgId.equals(organizationId))
                .orElse(false);
    }

    /**
     * Retorna informações completas do token (debug)
     */
    public String getTokenInfo() {
        return String.format(
                "User: %s (%s) | Org: %s (%s) | Roles: %s",
                getFullName(),
                getEmail(),
                getOrganizationName().orElse("N/A"),
                getOrganizationId().orElse("N/A"),
                String.join(", ", getRoles())
        );
    }

    /**
     * Verifica se o usuário está autenticado
     */
    public boolean isAuthenticated() {
        return securityIdentity != null && !securityIdentity.isAnonymous();
    }

    /**
     * Retorna o token JWT completo (para debug ou propagação)
     */
    public String getRawToken() {
        return jwt.getRawToken();
    }
}
