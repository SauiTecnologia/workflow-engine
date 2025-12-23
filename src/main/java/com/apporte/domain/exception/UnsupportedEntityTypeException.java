package com.apporte.domain.exception;

/**
 * Exceção quando tipo de entidade não é suportado
 */
public class UnsupportedEntityTypeException extends WorkflowException {
    public UnsupportedEntityTypeException(String message) {
        super(message);
    }

    public UnsupportedEntityTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
