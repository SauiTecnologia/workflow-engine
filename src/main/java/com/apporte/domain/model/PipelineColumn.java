package com.apporte.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade que representa uma coluna dentro de um Pipeline
 */
@Entity
@Table(name = "pipeline_columns")
public class PipelineColumn extends PanacheEntity {
    
    @Column(name = "pipeline_id", nullable = false)
    public Long pipelineId;
    
    @Column(nullable = false)
    public String key;  // ex: "inscritos"
    
    @Column(nullable = false)
    public String name;  // ex: "Inscritos"
    
    @Column(nullable = false)
    public Integer position;
    
    @Column(name = "allowed_entity_types", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> allowedEntityTypes;  // ex: ["project"]
    
    @Column(name = "allowed_roles_view", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> allowedRolesView;
    
    @Column(name = "allowed_roles_move_in", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> allowedRolesMoveIn;
    
    @Column(name = "allowed_roles_move_out", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> allowedRolesMoveOut;
    
    @Column(name = "transition_rules_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> transitionRulesJson;
    
    @Column(name = "notification_rules_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> notificationRulesJson;
    
    @Column(name = "card_layout_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> cardLayoutJson;
    
    @Column(name = "filter_config_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> filterConfigJson;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}