package com.apporte.domain.exception;

/**
 * Exceção quando tipo de entidade não é permitido na coluna
 */
public class InvalidEntityTypeException extends WorkflowException {
    public InvalidEntityTypeException(String message) {
        super(message);
    }

    public InvalidEntityTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
