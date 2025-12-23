package com.apporte.command;

/**
 * Resultado da execução de um comando
 */
public class CommandResult {
    private boolean success;
    private String message;
    private Object data;

    public CommandResult(boolean success, String message) {
        this(success, message, null);
    }

    public CommandResult(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
