package com.apporte.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade JPA para representar um Card em uma Pipeline.
 * Mapeia a tabela pipeline_cards no banco de dados.
 */
@Entity
@Table(name = "pipeline_cards")
public class PipelineCardEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;
    
    @Column(name = "column_id", nullable = false)
    private Long columnId;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "assigned_to")
    private String assignedTo;
    
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Construtor padrão.
     */
    public PipelineCardEntity() {
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Construtor com parâmetros básicos.
     */
    public PipelineCardEntity(Long pipelineId, Long columnId, String title, Integer sortOrder) {
        this();
        this.pipelineId = Objects.requireNonNull(pipelineId, "Pipeline ID não pode ser null");
        this.columnId = Objects.requireNonNull(columnId, "Column ID não pode ser null");
        this.title = Objects.requireNonNull(title, "Title não pode ser null");
        this.sortOrder = Objects.requireNonNull(sortOrder, "Sort order não pode ser null");
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPipelineId() {
        return pipelineId;
    }
    
    public void setPipelineId(Long pipelineId) {
        this.pipelineId = pipelineId;
    }
    
    public Long getColumnId() {
        return columnId;
    }
    
    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "PipelineCardEntity{" +
                "id=" + id +
                ", pipelineId=" + pipelineId +
                ", columnId=" + columnId +
                ", title='" + title + '\'' +
                ", sortOrder=" + sortOrder +
                ", createdAt=" + createdAt +
                '}';
    }
}
