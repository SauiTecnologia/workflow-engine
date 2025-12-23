package com.apporte.domain.exception;

/**
 * Exceção quando transição entre colunas não é permitida
 */
public class InvalidTransitionException extends WorkflowException {
    public InvalidTransitionException(String message) {
        super(message);
    }

    public InvalidTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
