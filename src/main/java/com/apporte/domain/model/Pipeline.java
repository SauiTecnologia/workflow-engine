package com.apporte.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade que representa um Pipeline (Kanban board)
 */
@Entity
@Table(name = "pipelines")
public class Pipeline extends PanacheEntity {
    
    @Column(nullable = false)
    public String name;
    
    @Column(name = "context_type", nullable = false)
    public String contextType;  // ex: "edital"
    
    @Column(name = "context_id", nullable = false)
    public String contextId;    // ex: "edital-123"
    
    @Column(name = "allowed_roles_view", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> allowedRolesView;
    
    @Column(name = "allowed_roles_manage", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> allowedRolesManage;
    
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