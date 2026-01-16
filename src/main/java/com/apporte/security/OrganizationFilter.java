package com.apporte.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import io.quarkus.logging.Log;

/**
 * Filtro que valida multi-tenancy para organizações.
 * 
 * Endpoints que requerem organização devem adicionar anotação @RequireOrganization
 * ou incluir header X-Organization-ID na requisição.
 */
@Provider
@Priority(2000)
public class OrganizationFilter implements ContainerRequestFilter {

    @Inject
    KeycloakUserContext userContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Skip para endpoints públicos
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/q/health") || path.startsWith("/q/metrics")) {
            return;
        }

        // Verifica se usuário está autenticado
        if (!userContext.isAuthenticated()) {
            return; // Deixa OIDC tratar a autenticação
        }

        // Admin do sistema pode acessar qualquer organização
        if (userContext.isSystemAdmin()) {
            Log.debug("System admin detected - bypassing organization filter");
            return;
        }

        // Extrai org_id do header (para multi-org users)
        String requestedOrgId = requestContext.getHeaderString("X-Organization-ID");
        
        if (requestedOrgId != null && !requestedOrgId.isEmpty()) {
            // Valida se usuário pertence à organização solicitada
            if (!userContext.belongsToOrganization(requestedOrgId)) {
                Log.warn(String.format(
                        "User %s tried to access organization %s but belongs to %s",
                        userContext.getEmail(),
                        requestedOrgId,
                        userContext.getOrganizationId().orElse("none")
                ));
                
                requestContext.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                                .entity("Access denied: User does not belong to this organization")
                                .build()
                );
            }
        }

        Log.debug(String.format("Request authorized: %s", userContext.getTokenInfo()));
    }
}
