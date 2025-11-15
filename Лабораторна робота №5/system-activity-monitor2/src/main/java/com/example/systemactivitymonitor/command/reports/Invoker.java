package com.example.systemactivitymonitor.command.reports;

import java.util.Stack;

/**
 * ⚙️ Invoker — керує виконанням команд.
 * Зберігає історію та дозволяє виконати Undo.
 */
public class Invoker {

    private final Stack<ReportCommand> history = new Stack<>();

    /** Виконати команду та додати її в історію */
    public void executeCommand(ReportCommand command) {
        command.execute();
        history.push(command);
    }

    /** Скасувати останню команду */
    public void undoLastCommand() {
        if (!history.isEmpty()) {
            ReportCommand last = history.pop();
            try {
                last.undo();
            } catch (UnsupportedOperationException e) {
                System.out.println("⚠️ Ця команда не підтримує Undo.");
            }
        } else {
            System.out.println("ℹ️ Історія команд порожня, нічого скасовувати.");
        }
    }

    /** Очистити історію */
    public void clearHistory() {
        history.clear();
    }
}
