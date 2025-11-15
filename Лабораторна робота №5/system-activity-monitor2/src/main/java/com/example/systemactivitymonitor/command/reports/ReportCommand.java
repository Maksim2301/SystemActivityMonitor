package com.example.systemactivitymonitor.command.reports;

/**
 * 🔹 Базовий інтерфейс шаблону Команда для операцій над звітами.
 * Кожна команда повинна реалізувати метод execute(),
 * а опціонально — undo() для підтримки скасування.
 */
public interface ReportCommand {
    void execute();

    default void undo() {
        throw new UnsupportedOperationException("Undo не підтримується для цієї команди.");
    }
}
