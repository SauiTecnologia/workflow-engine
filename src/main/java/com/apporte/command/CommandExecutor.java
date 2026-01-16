package com.apporte.command;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Stack;

/**
 * Executor para comandos de workflow.
 * Implementa pattern Command com suporte a undo.
 * 
 * @since 1.0
 */
@ApplicationScoped
public class CommandExecutor {
    private final Stack<WorkflowCommand> commandHistory = new Stack<>();

    /**
     * Executa um comando e o adiciona ao histórico.
     * 
     * @param command comando a ser executado
     * @return resultado da execução
     * @throws RuntimeException se houver erro na execução
     */
    public CommandResult execute(WorkflowCommand command) {
        try {
            command.execute();
            commandHistory.push(command);
            Log.infof("Command executed: %s", command.getClass().getSimpleName());
            return command.getResult();
        } catch (Exception e) {
            Log.errorf(e, "Error executing command: %s", command.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * Desfaz o último comando executado.
     * 
     * @throws IllegalStateException se não houver comandos para desfazer
     */
    public void undo() {
        if (commandHistory.isEmpty()) {
            throw new IllegalStateException("No commands to undo");
        }
        
        var command = commandHistory.pop();
        try {
            command.undo();
            Log.infof("Command undone: %s", command.getClass().getSimpleName());
        } catch (Exception e) {
            Log.errorf(e, "Error undoing command: %s", command.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * Verifica se há comandos no histórico.
     */
    public boolean hasHistory() {
        return !commandHistory.isEmpty();
    }

    /**
     * Limpa o histórico.
     */
    public void clearHistory() {
        commandHistory.clear();
    }
}
