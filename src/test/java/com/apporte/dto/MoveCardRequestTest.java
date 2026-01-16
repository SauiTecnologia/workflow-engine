package com.apporte.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para MoveCardRequest DTO (record).
 * Valida comportamento básico e acessores.
 */
@DisplayName("MoveCardRequest DTO")
class MoveCardRequestTest {
    
    @Test
    @DisplayName("deve criar validamente com parâmetros válidos")
    void testConstruction_WithValidData_CreatesSuccessfully() {
        // Act
        var request = new MoveCardRequest("1", "2");
        
        // Assert
        assertEquals("1", request.fromColumnId());
        assertEquals("2", request.toColumnId());
    }
    
    @Test
    @DisplayName("deve aceitar null no construtor")
    void testConstruction_WithNull_CreatesWithNull() {
        // Act
        var request = new MoveCardRequest(null, "2");
        
        // Assert
        assertNull(request.fromColumnId());
        assertEquals("2", request.toColumnId());
    }
    
    @Test
    @DisplayName("deve aceitar vazio no construtor")
    void testConstruction_WithBlank_CreatesWithBlank() {
        // Act
        var request = new MoveCardRequest("", "2");
        
        // Assert
        assertEquals("", request.fromColumnId());
        assertEquals("2", request.toColumnId());
    }
    
    @Test
    @DisplayName("deve gerar toString corretamente")
    void testToString_GeneratesCorrectly() {
        // Arrange
        var request = new MoveCardRequest("1", "2");
        
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
    @DisplayName("deve criar com valores iguais")
    void testConstruction_WithSameValues_CreatesSuccessfully() {
        // Act
        var request = new MoveCardRequest("5", "5");
        
        // Assert
        assertEquals("5", request.fromColumnId());
        assertEquals("5", request.toColumnId());
    }
    
    @Test
    @DisplayName("deve implementar equals corretamente")
    void testEquals_WithSameValues_ReturnsTrue() {
        // Arrange
        var request1 = new MoveCardRequest("1", "2");
        var request2 = new MoveCardRequest("1", "2");
        
        // Assert
        assertEquals(request1, request2);
    }
    
    @Test
    @DisplayName("deve implementar hashCode corretamente")
    void testHashCode_WithSameValues_ReturnsSameHash() {
        // Arrange
        var request1 = new MoveCardRequest("1", "2");
        var request2 = new MoveCardRequest("1", "2");
        
        // Assert
        assertEquals(request1.hashCode(), request2.hashCode());
    }
    
    @Test
    @DisplayName("record deve ser imutável")
    void testImmutability() {
        // Arrange
        var request = new MoveCardRequest("1", "2");
        
        // Assert - record não tem setters
        assertEquals("1", request.fromColumnId());
        assertEquals("2", request.toColumnId());
        // Não há como modificar os valores após criação
    }
}

