package com.apporte.domain.exception;

/**
 * Exceção base para erros de workflow
 */
public abstract class WorkflowException extends RuntimeException {
    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
