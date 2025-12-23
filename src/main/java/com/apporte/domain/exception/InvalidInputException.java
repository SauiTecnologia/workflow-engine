package com.apporte.domain.exception;

/**
 * Exceção quando entrada é inválida
 */
public class InvalidInputException extends WorkflowException {
    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
