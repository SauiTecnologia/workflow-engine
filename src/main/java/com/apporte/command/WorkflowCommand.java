package com.apporte.command;

/**
 * Interface para Command Pattern
 * Encapsula operações de workflow com suporte a histórico e undo
 */
public interface WorkflowCommand {
    
    /**
     * Executa o comando
     */
    void execute();
    
    /**
     * Desfaz o comando
     */
    void undo();
    
    /**
     * Obtém o resultado da execução
     */
    CommandResult getResult();
}
