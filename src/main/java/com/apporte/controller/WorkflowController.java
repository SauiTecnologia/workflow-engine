package com.apporte.controller;

import com.apporte.domain.model.Pipeline;
import com.apporte.domain.model.PipelineColumn;
import com.apporte.domain.model.UserContext;
import com.apporte.dto.MoveCardRequest;
import com.apporte.security.KeycloakUserContext;
import com.apporte.service.WorkflowService;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * RESTful controller for Workflow/Kanban operations.
 * 
 * <p>This controller provides endpoints for managing pipelines, columns, and cards
 * in a Kanban-style workflow system. All endpoints require authentication via Keycloak.
 * 
 * <p><strong>Security:</strong>
 * <ul>
 *   <li>All endpoints require authentication (@Authenticated)</li>
 *   <li>Organization-scoped endpoints validate org membership</li>
 *   <li>Role-based access control via @RolesAllowed</li>
 * </ul>
 * 
 * @since 2.0
 */
@Path("/api/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class WorkflowController {
    
    private final KeycloakUserContext keycloakUserContext;
    private final WorkflowService workflowService;
    
    @Inject
    public WorkflowController(
        KeycloakUserContext keycloakUserContext,
        WorkflowService workflowService
    ) {
        this.keycloakUserContext = keycloakUserContext;
        this.workflowService = workflowService;
    }
    
    /**
     * Retrieves a pipeline with its columns and cards.
     * 
     * <p><strong>Authorization:</strong> Any authenticated user can view pipelines
     * from their organization. System admins can view any pipeline.
     *
     * @param pipelineId the ID of the pipeline to retrieve
     * @return response containing pipeline details with columns
     * @throws NotFoundException if pipeline doesn't exist
     * @throws ForbiddenException if user doesn't have access to the pipeline's organization
     */
    @GET
    @Path("/{pipelineId}")
    public Response getPipeline(@PathParam("pipelineId") Long pipelineId) {
        Log.debugf("Loading pipeline %d for user %s", pipelineId, keycloakUserContext.getEmail());
        
        try {
            var userContext = UserContext.fromKeycloak(keycloakUserContext);
            var pipeline = workflowService.getPipeline(pipelineId);
            
            // Validate organization access
            validateOrganizationAccess(pipeline.getOrganizationId(), userContext);
            
            var columns = workflowService.getPipelineColumns(pipelineId);
            
            Log.infof("Pipeline %d loaded successfully for user %s", 
                pipelineId, keycloakUserContext.getEmail());
            
            return Response.ok(new PipelineResponse(pipeline, columns)).build();
            
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid argument: %s", e.getMessage());
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (ForbiddenException e) {
            Log.warnf("Access denied to pipeline %d for user %s: %s",
                pipelineId, keycloakUserContext.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Error loading pipeline %d", pipelineId);
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to load pipeline"))
                .build();
        }
    }
    
    /**
     * Moves a card from one column to another within a pipeline.
     * 
     * <p><strong>Authorization:</strong> Requires 'proponente' or 'org-admin' role.
     * User must belong to the same organization as the pipeline.
     *
     * @param pipelineId the ID of the pipeline containing the card
     * @param cardId the ID of the card to move
     * @param request the move request containing target column and position
     * @return response containing the updated card state
     * @throws NotFoundException if pipeline or card doesn't exist
     * @throws ForbiddenException if user doesn't have permission
     */
    @POST
    @Path("/{pipelineId}/cards/{cardId}/move")
    @RolesAllowed({"proponente", "org-admin", "system-admin"})
    public Response moveCard(
        @PathParam("pipelineId") Long pipelineId,
        @PathParam("cardId") Long cardId,
        MoveCardRequest request
    ) {
        Log.debugf("Moving card %d in pipeline %d by user %s", 
            cardId, pipelineId, keycloakUserContext.getEmail());
        
        try {
            var userContext = UserContext.fromKeycloak(keycloakUserContext);
            var pipeline = workflowService.getPipeline(pipelineId);
            
            // Validate organization access
            validateOrganizationAccess(pipeline.getOrganizationId(), userContext);
            
            var response = workflowService.moveCard(pipelineId, cardId, request, userContext);
            
            Log.infof("Card %d moved successfully by user %s", 
                cardId, keycloakUserContext.getEmail());
            
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid move card request: %s", e.getMessage());
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (ForbiddenException e) {
            Log.warnf("Access denied for card move: %s", e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Error moving card %d", cardId);
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to move card"))
                .build();
        }
    }
    
    /**
     * Retrieves details of a specific card.
     * 
     * <p><strong>Authorization:</strong> Any authenticated user from the card's
     * organization can view it. System admins can view any card.
     *
     * @param pipelineId the ID of the pipeline containing the card
     * @param cardId the ID of the card to retrieve
     * @return response containing card details
     * @throws NotFoundException if card doesn't exist
     * @throws ForbiddenException if user doesn't have access
     */
    @GET
    @Path("/{pipelineId}/cards/{cardId}")
    public Response getCard(
        @PathParam("pipelineId") Long pipelineId,
        @PathParam("cardId") Long cardId
    ) {
        Log.debugf("Fetching card %d from pipeline %d for user %s",
            cardId, pipelineId, keycloakUserContext.getEmail());
        
        try {
            var userContext = UserContext.fromKeycloak(keycloakUserContext);
            var pipeline = workflowService.getPipeline(pipelineId);
            
            // Validate organization access
            validateOrganizationAccess(pipeline.getOrganizationId(), userContext);
            
            var card = workflowService.getPipelineCard(cardId);
            
            Log.infof("Card %d retrieved for user %s", cardId, keycloakUserContext.getEmail());
            
            return Response.ok(card).build();
            
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid card request: %s", e.getMessage());
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (ForbiddenException e) {
            Log.warnf("Access denied to card %d: %s", cardId, e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Error fetching card %d", cardId);
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to fetch card"))
                .build();
        }
    }
    
    /**
     * Validates that user has access to the specified organization.
     * System admins bypass this check.
     * 
     * @param pipelineOrgId the organization ID from the pipeline
     * @param userContext the current user context
     * @throws ForbiddenException if user doesn't have access
     */
    private void validateOrganizationAccess(String pipelineOrgId, UserContext userContext) {
        // System admins can access any organization
        if (userContext.isSystemAdmin()) {
            return;
        }
        
        // Check if user belongs to an organization
        if (!userContext.hasOrganization()) {
            throw new ForbiddenException("User must belong to an organization");
        }
        
        // Check if user's organization matches pipeline's organization
        if (!userContext.organizationId().equals(pipelineOrgId)) {
            throw new ForbiddenException(
                "Access denied: user does not belong to pipeline's organization"
            );
        }
    }
    
    /**
     * Response DTO for pipeline data.
     * 
     * @param pipeline the pipeline entity
     * @param columns the list of columns in the pipeline
     */
    public record PipelineResponse(
        Pipeline pipeline,
        List<PipelineColumn> columns
    ) {}
    
    /**
     * Response DTO for error messages.
     * 
     * @param error the error message
     * @param timestamp when the error occurred
     */
    public record ErrorResponse(
        String error,
        long timestamp
    ) {
        public ErrorResponse(String error) {
            this(error, System.currentTimeMillis());
        }
    }
}
