package ru.fintrack.validation;

/**
 * Базовая валидация пользовательского ввода.
 */
public final class InputValidator {

    private InputValidator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("Поле \"" + fieldName + "\" не может быть пустым.");
        }
    }

    public static double parsePositiveAmount(String raw) {
        if (isBlank(raw)) {
            throw new IllegalArgumentException("Сумма не может быть пустой.");
        }
        try {
            double amount = Double.parseDouble(raw.trim().replace(',', '.'));
            if (amount <= 0) {
                throw new IllegalArgumentException("Сумма должна быть больше нуля.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат суммы: " + raw);
        }
    }
}
