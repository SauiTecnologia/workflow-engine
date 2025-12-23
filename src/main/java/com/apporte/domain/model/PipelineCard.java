package com.apporte.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade que representa um card dentro de uma coluna
 */
@Entity
@Table(name = "pipeline_cards")
public class PipelineCard extends PanacheEntity {
    
    @Column(name = "pipeline_id", nullable = false)
    public Long pipelineId;
    
    @Column(name = "column_id", nullable = false)
    public Long columnId;
    
    @Column(name = "entity_type", nullable = false)
    public String entityType;  // ex: "project"
    
    @Column(name = "entity_id", nullable = false)
    public String entityId;    // ex: "proj-1"
    
    @Column(name = "sort_order")
    public Integer sortOrder;
    
    @Column(name = "data_snapshot_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> dataSnapshotJson;
    
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