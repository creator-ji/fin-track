package ru.fintrack.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import ru.fintrack.validation.InputValidator;

/**
 * Чтение и первичная валидация пользовательского ввода.
 */
public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public String readRequired(String prompt, String fieldName) {
        String value = readLine(prompt);
        InputValidator.requireNonBlank(value, fieldName);
        return value.trim();
    }

    public double readPositiveAmount(String prompt) {
        return InputValidator.parsePositiveAmount(readLine(prompt));
    }

    public List<String> readCategoryList(String prompt) {
        String raw = readLine(prompt);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Список категорий пуст.");
        }
        List<String> names = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Список категорий пуст.");
        }
        return names;
    }
}
