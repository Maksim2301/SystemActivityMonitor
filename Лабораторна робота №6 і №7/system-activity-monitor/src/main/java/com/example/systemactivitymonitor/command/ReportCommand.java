package com.example.systemactivitymonitor.command;

public abstract class ReportCommand {

    public abstract boolean execute();

    public void undo() {
        throw new UnsupportedOperationException("Undo не підтримується.");
    }
}
