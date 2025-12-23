package com.apporte.command;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Stack;

/**
 * Executor para comandos de workflow
 * Mantém histórico de comandos para suportar undo
 */
@ApplicationScoped
public class CommandExecutor {
    private Stack<WorkflowCommand> commandHistory = new Stack<>();

    /**
     * Executa um comando
     */
    public CommandResult execute(WorkflowCommand command) {
        try {
            command.execute();
            commandHistory.push(command);
            Log.info("Command executed: " + command.getClass().getSimpleName());
            return command.getResult();
        } catch (Exception e) {
            Log.error("Error executing command: " + command.getClass().getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Desfaz o último comando executado
     */
    public void undo() {
        if (!commandHistory.isEmpty()) {
            WorkflowCommand command = commandHistory.pop();
            try {
                command.undo();
                Log.info("Command undone: " + command.getClass().getSimpleName());
            } catch (Exception e) {
                Log.error("Error undoing command: " + command.getClass().getSimpleName(), e);
                throw e;
            }
        }
    }

    /**
     * Obtém o tamanho do histórico
     */
    public int getHistorySize() {
        return commandHistory.size();
    }

    /**
     * Limpa o histórico
     */
    public void clearHistory() {
        commandHistory.clear();
    }
}
