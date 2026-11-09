package ru.fintrack;

import ru.fintrack.ui.ConsoleApplication;

/**
 * Точка входа приложения.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ConsoleApplication.createDefault().run();
    }
}
