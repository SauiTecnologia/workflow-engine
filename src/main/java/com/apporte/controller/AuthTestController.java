package com.apporte.controller;

import com.apporte.security.KeycloakUserContext;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Endpoint de exemplo demonstrando integração com Keycloak.
 * 
 * Exemplos de uso:
 * - @Authenticated: Qualquer usuário autenticado
 * - @RolesAllowed: Apenas roles específicas
 * - KeycloakUserContext: Acesso a dados do usuário
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthTestController {

    private final KeycloakUserContext userContext;

    @Inject
    public AuthTestController(KeycloakUserContext userContext) {
        this.userContext = userContext;
    }

    /**
     * Endpoint público de teste (sem autenticação)
     */
    @GET
    @Path("/public")
    public Response publicEndpoint() {
        return Response.ok(Map.of(
                "message", "Este endpoint é público",
                "timestamp", System.currentTimeMillis()
        )).build();
    }

    /**
     * Endpoint protegido - qualquer usuário autenticado
     */
    @GET
    @Path("/me")
    @Authenticated
    public Response getUserInfo() {
        var response = new java.util.HashMap<String, Object>();
        response.put("userId", userContext.getUserId());
        response.put("email", userContext.getEmail());
        response.put("name", userContext.getFullName());
        response.put("username", userContext.getUsername());
        response.put("organizationId", userContext.getOrganizationId().orElse(null));
        response.put("organizationName", userContext.getOrganizationName().orElse(null));
        response.put("groups", userContext.getGroups());
        response.put("roles", userContext.getRoles());
        response.put("isSystemAdmin", userContext.isSystemAdmin());
        response.put("isOrgAdmin", userContext.isOrgAdmin());
        response.put("isProponente", userContext.isProponente());
        response.put("isParecerista", userContext.isParecerista());
        response.put("isInvestidor", userContext.isInvestidor());
        return Response.ok(response).build();
    }

    /**
     * Endpoint apenas para admins do sistema
     */
    @GET
    @Path("/admin")
    @RolesAllowed("system-admin")
    public Response adminOnly() {
        return Response.ok(Map.of(
                "message", "Você é um administrador do sistema!",
                "user", userContext.getFullName(),
                "roles", userContext.getRoles()
        )).build();
    }

    /**
     * Endpoint para proponentes
     */
    @GET
    @Path("/proponente")
    @RolesAllowed({"proponente", "org-admin", "system-admin"})
    public Response proponenteOnly() {
        return Response.ok(Map.of(
                "message", "Acesso liberado para proponentes",
                "user", userContext.getFullName(),
                "organization", userContext.getOrganizationName().orElse("N/A"),
                "canCreateProposals", true
        )).build();
    }

    /**
     * Endpoint para pareceristas
     */
    @GET
    @Path("/parecerista")
    @RolesAllowed({"parecerista", "org-admin", "system-admin"})
    public Response pareceristaOnly() {
        return Response.ok(Map.of(
                "message", "Acesso liberado para pareceristas",
                "user", userContext.getFullName(),
                "organization", userContext.getOrganizationName().orElse("N/A"),
                "canReviewProposals", true
        )).build();
    }

    /**
     * Endpoint para investidores
     */
    @GET
    @Path("/investidor")
    @RolesAllowed({"investidor", "org-admin", "system-admin"})
    public Response investidorOnly() {
        return Response.ok(Map.of(
                "message", "Acesso liberado para investidores",
                "user", userContext.getFullName(),
                "organization", userContext.getOrganizationName().orElse("N/A"),
                "canInvest", true
        )).build();
    }

    /**
     * Endpoint para verificar organização específica
     */
    @GET
    @Path("/org-check")
    @Authenticated
    public Response checkOrganization() {
        String orgId = userContext.getOrganizationId().orElse(null);
        
        if (orgId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "error", "Usuário não pertence a nenhuma organização"
                    ))
                    .build();
        }

        return Response.ok(Map.of(
                "organizationId", orgId,
                "organizationName", userContext.getOrganizationName().orElse("N/A"),
                "user", userContext.getFullName(),
                "roles", userContext.getRoles(),
                "groups", userContext.getGroups()
        )).build();
    }

    /**
     * Endpoint de debug - mostra token info completa
     */
    @GET
    @Path("/debug")
    @Authenticated
    public Response debug() {
        return Response.ok(Map.of(
                "tokenInfo", userContext.getTokenInfo(),
                "isAuthenticated", userContext.isAuthenticated(),
                "allClaims", Map.of(
                        "userId", userContext.getUserId(),
                        "email", userContext.getEmail(),
                        "fullName", userContext.getFullName(),
                        "firstName", userContext.getFirstName(),
                        "lastName", userContext.getLastName(),
                        "username", userContext.getUsername(),
                        "organizationId", userContext.getOrganizationId().orElse(null),
                        "organizationName", userContext.getOrganizationName().orElse(null),
                        "groups", userContext.getGroups(),
                        "roles", userContext.getRoles()
                )
        )).build();
    }
}
