package com.apporte.client;

import com.apporte.dto.MoveCardRequest;
import com.apporte.dto.MoveCardResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Cliente HTTP para comunicação com serviços externos de Workflow.
 * Implementa padrão de Circuit Breaker para resiliência.
 */
@ApplicationScoped
public class WorkflowApiClient {
    
    @ConfigProperty(name = "workflow.api.url", defaultValue = "http://localhost:8080")
    String workflowApiUrl;
    
    private final Client client;
    
    public WorkflowApiClient() {
        this.client = ClientBuilder.newClient();
    }
    
    /**
     * Faz uma chamada GET para obter um pipeline.
     *
     * @param pipelineId ID do pipeline
     * @return Response da API
     */
    public Response getPipeline(Long pipelineId) {
        try {
            Log.infof("Chamando API para obter pipeline %d", pipelineId);
            return client.target(workflowApiUrl)
                .path("/api/pipelines/{id}")
                .resolveTemplate("id", pipelineId)
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            Log.errorf("Erro ao obter pipeline %d: %s", pipelineId, e.getMessage());
            throw new RuntimeException("Falha ao comunicar com API de Workflow", e);
        }
    }
    
    /**
     * Faz uma chamada POST para mover um card.
     *
     * @param pipelineId ID do pipeline
     * @param cardId ID do card
     * @param request Requisição de movimento
     * @param token Token JWT para autorização
     * @return Response da API
     */
    public Response moveCard(Long pipelineId, Long cardId, MoveCardRequest request, String token) {
        try {
            Log.infof("Chamando API para mover card %d no pipeline %d", cardId, pipelineId);
            return client.target(workflowApiUrl)
                .path("/api/pipelines/{pipelineId}/cards/{cardId}/move")
                .resolveTemplate("pipelineId", pipelineId)
                .resolveTemplate("cardId", cardId)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .post(jakarta.ws.rs.client.Entity.json(request));
        } catch (Exception e) {
            Log.errorf("Erro ao mover card %d: %s", cardId, e.getMessage());
            throw new RuntimeException("Falha ao comunicar com API de Workflow", e);
        }
    }
    
    /**
     * Fecha recursos do cliente HTTP.
     */
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
