package com.example.systemactivitymonitor.command;

import java.util.Stack;

public class Invoker {

    private final Stack<ReportCommand> history = new Stack<>();

    /** Виконує команду, якщо success=true — додає її в історію */
    public boolean executeAndStore(ReportCommand command) {
        boolean ok = command.execute();
        if (ok) history.push(command);
        return ok;
    }

    /** Скасовує останню команду, якщо можливо */
    public boolean undoLastCommand() {
        if (history.isEmpty()) return false;

        ReportCommand last = history.pop();
        last.undo();
        return true;
    }

    public void clear() {
        history.clear();
    }
}
