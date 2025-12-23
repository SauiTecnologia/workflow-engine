package com.apporte.controller;

import com.apporte.domain.model.Pipeline;
import com.apporte.domain.model.PipelineCard;
import com.apporte.domain.model.PipelineColumn;
import com.apporte.domain.model.UserContext;
import com.apporte.dto.MoveCardRequest;
import com.apporte.dto.MoveCardResponse;
import com.apporte.security.JwtValidator;
import com.apporte.service.WorkflowService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Controller REST para operações de Workflow (Kanban).
 * Responsável por gerenciar requisições HTTP relacionadas a pipelines.
 */
@Path("/api/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowController {
    
    @Inject
    private JwtValidator jwtValidator;
    
    @Inject
    private WorkflowService workflowService;
    
    /**
     * GET /api/pipelines/{pipelineId}
     * Carrega um pipeline com suas colunas e cards.
     *
     * @param pipelineId ID do pipeline
     * @param headers Headers HTTP (incluindo Authorization)
     * @return Response com PipelineResponse
     */
    @GET
    @Path("/{pipelineId}")
    public Response getPipeline(
        @PathParam("pipelineId") Long pipelineId,
        @Context HttpHeaders headers
    ) {
        try {
            UserContext user = extractUserContext(headers);
            
            Pipeline pipeline = workflowService.getPipeline(pipelineId);
            List<PipelineColumn> columns = workflowService.getPipelineColumns(pipelineId);
            
            Log.infof("Pipeline %d carregado para usuário %s", pipelineId, user.getId());
            
            return Response.ok()
                .entity(new PipelineResponse(pipeline, columns))
                .build();
        } catch (IllegalArgumentException e) {
            Log.warnf("Argumento inválido: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            Log.errorf("Erro ao carregar pipeline: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }
    
    /**
     * POST /api/pipelines/{pipelineId}/cards/{cardId}/move
     * Move um card de uma coluna para outra.
     *
     * @param pipelineId ID do pipeline
     * @param cardId ID do card
     * @param request Requisição de movimento
     * @param headers Headers HTTP (incluindo Authorization)
     * @return Response com MoveCardResponse
     */
    @POST
    @Path("/{pipelineId}/cards/{cardId}/move")
    public Response moveCard(
        @PathParam("pipelineId") Long pipelineId,
        @PathParam("cardId") Long cardId,
        MoveCardRequest request,
        @Context HttpHeaders headers
    ) {
        try {
            UserContext user = extractUserContext(headers);
            
            MoveCardResponse response = workflowService.moveCard(
                pipelineId,
                cardId,
                request,
                user
            );
            
            Log.infof("Card %d movido por usuário %s", cardId, user.getId());
            
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            Log.warnf("Argumento inválido: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            Log.errorf("Erro ao mover card: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }
    
    /**
     * GET /api/pipelines/{pipelineId}/cards/{cardId}
     * Obtém detalhes de um card específico.
     *
     * @param pipelineId ID do pipeline
     * @param cardId ID do card
     * @param headers Headers HTTP
     * @return Response com detalhes do card
     */
    @GET
    @Path("/{pipelineId}/cards/{cardId}")
    public Response getCard(
        @PathParam("pipelineId") Long pipelineId,
        @PathParam("cardId") Long cardId,
        @Context HttpHeaders headers
    ) {
        try {
            UserContext user = extractUserContext(headers);
            
            PipelineCard card = workflowService.getPipelineCard(cardId);
            
            Log.infof("Card %d obtido para usuário %s", cardId, user.getId());
            
            return Response.ok(card).build();
        } catch (Exception e) {
            Log.errorf("Erro ao obter card: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }
    
    /**
     * Extrai UserContext do JWT no header Authorization.
     * Valida o token e retorna as informações do usuário.
     *
     * @param headers Headers HTTP
     * @return UserContext extraído do token
     * @throws IllegalArgumentException se o header não existir ou token inválido
     */
    private UserContext extractUserContext(HttpHeaders headers) {
        String authHeader = headers.getHeaderString("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            throw new IllegalArgumentException("Header Authorization é obrigatório");
        }
        
        return jwtValidator.validateAndExtract(authHeader);
    }
    
    /**
     * DTO para resposta de erro.
     */
    public static class ErrorResponse {
        public String error;
        public long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * DTO para resposta de pipeline.
     */
    public static class PipelineResponse {
        public Pipeline pipeline;
        public List<PipelineColumn> columns;
        
        public PipelineResponse(Pipeline pipeline, List<PipelineColumn> columns) {
            this.pipeline = pipeline;
            this.columns = columns;
        }
    }
}
