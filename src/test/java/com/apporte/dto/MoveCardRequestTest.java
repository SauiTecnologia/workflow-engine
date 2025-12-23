package com.apporte.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para MoveCardRequest DTO.
 * Valida comportamento básico e getters/setters.
 */
@DisplayName("MoveCardRequest DTO")
class MoveCardRequestTest {
    
    @Test
    @DisplayName("deve criar validamente com parâmetros válidos")
    void testConstruction_WithValidData_CreatesSuccessfully() {
        // Act
        MoveCardRequest request = new MoveCardRequest("1", "2");
        
        // Assert
        assertEquals("1", request.getFromColumnId());
        assertEquals("2", request.getToColumnId());
    }
    
    @Test
    @DisplayName("deve aceitar null no construtor")
    void testConstruction_WithNull_CreatesWithNull() {
        // Act
        MoveCardRequest request = new MoveCardRequest(null, "2");
        
        // Assert
        assertNull(request.getFromColumnId());
        assertEquals("2", request.getToColumnId());
    }
    
    @Test
    @DisplayName("deve aceitar vazio no construtor")
    void testConstruction_WithBlank_CreatesWithBlank() {
        // Act
        MoveCardRequest request = new MoveCardRequest("", "2");
        
        // Assert
        assertEquals("", request.getFromColumnId());
        assertEquals("2", request.getToColumnId());
    }
    
    @Test
    @DisplayName("deve usar setter corretamente")
    void testSetters_UpdateFieldsCorrectly() {
        // Arrange
        MoveCardRequest request = new MoveCardRequest("1", "2");
        
        // Act
        request.setFromColumnId("10");
        request.setToColumnId("20");
        
        // Assert
        assertEquals("10", request.getFromColumnId());
        assertEquals("20", request.getToColumnId());
    }
    
    @Test
    @DisplayName("deve gerar toString corretamente")
    void testToString_GeneratesCorrectly() {
        // Arrange
        MoveCardRequest request = new MoveCardRequest("1", "2");
        
        // Act
        String str = request.toString();
        
        // Assert
        assertTrue(str.contains("MoveCardRequest"));
        assertTrue(str.contains("fromColumnId"));
        assertTrue(str.contains("toColumnId"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
    }
    
    @Test
    @DisplayName("deve construir com construtor padrão")
    void testDefaultConstructor_CreatesValidObject() {
        // Act
        MoveCardRequest request = new MoveCardRequest();
        request.setFromColumnId("1");
        request.setToColumnId("2");
        
        // Assert
        assertEquals("1", request.getFromColumnId());
        assertEquals("2", request.getToColumnId());
    }
    
    @Test
    @DisplayName("deve criar com valores iguais")
    void testConstruction_WithSameValues_CreatesSuccessfully() {
        // Act
        MoveCardRequest request = new MoveCardRequest("5", "5");
        
        // Assert
        assertEquals("5", request.getFromColumnId());
        assertEquals("5", request.getToColumnId());
    }
    
    @Test
    @DisplayName("deve aceitar valores numéricos grandes")
    void testConstruction_WithLargeNumbers_CreatesSuccessfully() {
        // Act
        MoveCardRequest request = new MoveCardRequest("999999999", "888888888");
        
        // Assert
        assertEquals("999999999", request.getFromColumnId());
        assertEquals("888888888", request.getToColumnId());
    }
}

