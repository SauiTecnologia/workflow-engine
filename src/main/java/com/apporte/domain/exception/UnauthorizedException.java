package com.apporte.domain.exception;

/**
 * Exceção quando usuário não tem permissão para realizar operação
 */
public class UnauthorizedException extends WorkflowException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
