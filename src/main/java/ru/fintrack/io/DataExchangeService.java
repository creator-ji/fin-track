package ru.fintrack.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.TransactionType;
import ru.fintrack.model.Wallet;
import ru.fintrack.repository.UserStorage;

/**
 * Экспорт и импорт снимка кошелька в CSV и JSON (без внешних библиотек).
 */
public class DataExchangeService {

    public Path exportCsv(Wallet wallet, Path filePath) {
        StringBuilder csv = new StringBuilder();
        csv.append("section,type,category,amount,comment_or_limit\n");
        for (Transaction transaction : wallet.getTransactions()) {
            csv.append("TRANSACTION,")
                    .append(transaction.getType().name()).append(',')
                    .append(escapeCsv(transaction.getCategory().getName())).append(',')
                    .append(transaction.getAmount()).append(',')
                    .append(escapeCsv(nullToEmpty(transaction.getComment())))
                    .append('\n');
        }
        for (Budget budget : wallet.getBudgets().values()) {
            csv.append("BUDGET,,")
                    .append(escapeCsv(budget.getCategory().getName())).append(',')
                    .append(budget.getLimit()).append(',')
                    .append('\n');
        }
        return write(filePath, csv.toString());
    }

    public Path exportJson(Wallet wallet, Path filePath) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"transactions\": [\n");
        boolean firstTx = true;
        for (Transaction transaction : wallet.getTransactions()) {
            if (!firstTx) {
                json.append(",\n");
            }
            firstTx = false;
            json.append("    {")
                    .append("\"type\":\"").append(transaction.getType().name()).append("\",")
                    .append("\"category\":\"").append(escapeJson(transaction.getCategory().getName())).append("\",")
                    .append("\"amount\":").append(transaction.getAmount()).append(',')
                    .append("\"comment\":\"").append(escapeJson(nullToEmpty(transaction.getComment()))).append("\"")
                    .append('}');
        }
        json.append("\n  ],\n  \"budgets\": [\n");
        boolean firstBudget = true;
        for (Budget budget : wallet.getBudgets().values()) {
            if (!firstBudget) {
                json.append(",\n");
            }
            firstBudget = false;
            json.append("    {")
                    .append("\"category\":\"").append(escapeJson(budget.getCategory().getName())).append("\",")
                    .append("\"limit\":").append(budget.getLimit())
                    .append('}');
        }
        json.append("\n  ]\n}\n");
        return write(filePath, json.toString());
    }

    public void importCsv(Wallet wallet, Path filePath) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            Wallet loaded = new Wallet();
            String[] lines = content.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty() || i == 0 && line.toLowerCase(Locale.ROOT).startsWith("section,")) {
                    continue;
                }
                String[] parts = splitCsv(line);
                if (parts.length < 4) {
                    throw new IllegalArgumentException("Некорректная CSV-строка: " + line);
                }
                String section = parts[0].trim().toUpperCase(Locale.ROOT);
                if ("TRANSACTION".equals(section)) {
                    TransactionType type = TransactionType.valueOf(parts[1].trim());
                    Category category = new Category(unescapeCsv(parts[2]));
                    double amount = Double.parseDouble(parts[3].trim());
                    String comment = parts.length > 4 ? unescapeCsv(parts[4]) : "";
                    loaded.addTransaction(new Transaction(type, category, amount, comment.isBlank() ? null : comment));
                } else if ("BUDGET".equals(section)) {
                    Category category = new Category(unescapeCsv(parts[2]));
                    double limit = Double.parseDouble(parts[3].trim());
                    loaded.setBudget(new Budget(category, limit));
                } else {
                    throw new IllegalArgumentException("Неизвестная секция CSV: " + section);
                }
            }
            UserStorage.copyWallet(loaded, wallet);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось импортировать CSV: " + e.getMessage(), e);
        }
    }

    public void importJson(Wallet wallet, Path filePath) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8).trim();
            Wallet loaded = new Wallet();

            String txBlock = extractArray(content, "transactions");
            for (String object : splitObjects(txBlock)) {
                String type = readJsonString(object, "type");
                String category = readJsonString(object, "category");
                double amount = readJsonNumber(object, "amount");
                String comment = readJsonString(object, "comment");
                loaded.addTransaction(new Transaction(
                        TransactionType.valueOf(type),
                        new Category(category),
                        amount,
                        comment.isBlank() ? null : comment
                ));
            }

            String budgetBlock = extractArray(content, "budgets");
            for (String object : splitObjects(budgetBlock)) {
                String category = readJsonString(object, "category");
                double limit = readJsonNumber(object, "limit");
                loaded.setBudget(new Budget(new Category(category), limit));
            }

            UserStorage.copyWallet(loaded, wallet);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось импортировать JSON: " + e.getMessage(), e);
        }
    }

    private Path write(Path filePath, String content) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return filePath.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить файл: " + e.getMessage(), e);
        }
    }

    private static String extractArray(String json, String field) {
        String marker = "\"" + field + "\"";
        int fieldPos = json.indexOf(marker);
        if (fieldPos < 0) {
            return "";
        }
        int start = json.indexOf('[', fieldPos);
        int end = json.indexOf(']', start);
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("Некорректный JSON: массив \"" + field + "\".");
        }
        return json.substring(start + 1, end).trim();
    }

    private static java.util.List<String> splitObjects(String arrayBody) {
        java.util.List<String> objects = new java.util.ArrayList<>();
        if (arrayBody.isBlank()) {
            return objects;
        }
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < arrayBody.length(); i++) {
            char ch = arrayBody.charAt(i);
            if (ch == '{') {
                depth++;
            }
            if (depth > 0) {
                current.append(ch);
            }
            if (ch == '}') {
                depth--;
                if (depth == 0) {
                    objects.add(current.toString());
                    current.setLength(0);
                }
            }
        }
        return objects;
    }

    private static String readJsonString(String object, String field) {
        String marker = "\"" + field + "\"";
        int pos = object.indexOf(marker);
        if (pos < 0) {
            return "";
        }
        int colon = object.indexOf(':', pos);
        int firstQuote = object.indexOf('"', colon + 1);
        int secondQuote = object.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) {
            throw new IllegalArgumentException("Не удалось прочитать поле \"" + field + "\".");
        }
        return unescapeJson(object.substring(firstQuote + 1, secondQuote));
    }

    private static double readJsonNumber(String object, String field) {
        String marker = "\"" + field + "\"";
        int pos = object.indexOf(marker);
        if (pos < 0) {
            throw new IllegalArgumentException("Поле \"" + field + "\" не найдено.");
        }
        int colon = object.indexOf(':', pos);
        int end = colon + 1;
        while (end < object.length() && Character.isWhitespace(object.charAt(end))) {
            end++;
        }
        int start = end;
        while (end < object.length() && "0123456789.-".indexOf(object.charAt(end)) >= 0) {
            end++;
        }
        return Double.parseDouble(object.substring(start, end));
    }

    private static String[] splitCsv(String line) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (ch == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        parts.add(current.toString());
        return parts.toArray(String[]::new);
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String unescapeCsv(String value) {
        return value == null ? "" : value;
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
