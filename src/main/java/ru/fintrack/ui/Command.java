package ru.fintrack.ui;

/**
 * Команда CLI (паттерн Command).
 */
@FunctionalInterface
public interface Command {

    /**
     * @return {@code true}, если приложение должно продолжить работу
     */
    boolean execute(ConsoleSession session);
}
