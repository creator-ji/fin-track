package ru.fintrack.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.TransactionType;
import ru.fintrack.model.User;
import ru.fintrack.model.Wallet;

/**
 * Сохранение и загрузка пользователя с кошельком в файл.
 * Формат: секции USER / TRANSACTIONS / BUDGETS.
 */
public class WalletFileStorage {

    private static final String SECTION_USER = "#USER";
    private static final String SECTION_TRANSACTIONS = "#TRANSACTIONS";
    private static final String SECTION_BUDGETS = "#BUDGETS";

    private final Path dataDirectory;

    public WalletFileStorage(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public Path resolveUserFile(String login) {
        return dataDirectory.resolve(sanitizeFileName(login) + ".wallet");
    }

    public void save(User user) {
        Path file = resolveUserFile(user.getLogin());
        try {
            Files.createDirectories(dataDirectory);
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(SECTION_USER);
                writer.newLine();
                writer.write("login=" + escape(user.getLogin()));
                writer.newLine();
                writer.write("password=" + escape(user.getPassword()));
                writer.newLine();

                writer.write(SECTION_TRANSACTIONS);
                writer.newLine();
                for (Transaction transaction : user.getWallet().getTransactions()) {
                    writer.write(transaction.getType().name());
                    writer.write('|');
                    writer.write(escape(transaction.getCategory().getName()));
                    writer.write('|');
                    writer.write(Double.toString(transaction.getAmount()));
                    writer.write('|');
                    writer.write(escape(transaction.getComment() == null ? "" : transaction.getComment()));
                    writer.newLine();
                }

                writer.write(SECTION_BUDGETS);
                writer.newLine();
                for (Budget budget : user.getWallet().getBudgets().values()) {
                    writer.write(escape(budget.getCategory().getName()));
                    writer.write('|');
                    writer.write(Double.toString(budget.getLimit()));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить данные пользователя \""
                    + user.getLogin() + "\": " + e.getMessage(), e);
        }
    }

    public Optional<User> load(String login) {
        Path file = resolveUserFile(login);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String storedLogin = null;
            String password = null;
            Wallet wallet = new Wallet();
            String section = null;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("#")) {
                    section = line.trim();
                    continue;
                }

                if (SECTION_USER.equals(section)) {
                    if (line.startsWith("login=")) {
                        storedLogin = unescape(line.substring("login=".length()));
                    } else if (line.startsWith("password=")) {
                        password = unescape(line.substring("password=".length()));
                    }
                } else if (SECTION_TRANSACTIONS.equals(section)) {
                    wallet.addTransaction(parseTransaction(line));
                } else if (SECTION_BUDGETS.equals(section)) {
                    wallet.setBudget(parseBudget(line));
                }
            }

            if (storedLogin == null || password == null) {
                throw new IllegalStateException("Повреждённый файл пользователя: " + file);
            }

            User user = new User(storedLogin, password);
            copyWallet(wallet, user.getWallet());
            return Optional.of(user);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить данные пользователя \""
                    + login + "\": " + e.getMessage(), e);
        }
    }

    public void loadWalletInto(User user) {
        Optional<User> loaded = load(user.getLogin());
        if (loaded.isEmpty()) {
            return;
        }
        copyWallet(loaded.get().getWallet(), user.getWallet());
    }

    public static void copyWallet(Wallet source, Wallet target) {
        target.clear();
        for (Transaction transaction : source.getTransactions()) {
            target.addTransaction(transaction);
        }
        for (Budget budget : source.getBudgets().values()) {
            target.setBudget(budget);
        }
    }

    private static Transaction parseTransaction(String line) {
        String[] parts = splitFields(line, 4);
        TransactionType type = TransactionType.valueOf(parts[0]);
        Category category = new Category(unescape(parts[1]));
        double amount = Double.parseDouble(parts[2]);
        String comment = unescape(parts[3]);
        if (comment.isEmpty()) {
            comment = null;
        }
        return new Transaction(type, category, amount, comment);
    }

    private static Budget parseBudget(String line) {
        String[] parts = splitFields(line, 2);
        Category category = new Category(unescape(parts[0]));
        double limit = Double.parseDouble(parts[1]);
        return new Budget(category, limit);
    }

    private static String[] splitFields(String line, int expected) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (escaped) {
                current.append('\\').append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '|') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (escaped) {
            current.append('\\');
        }
        parts.add(current.toString());

        if (parts.size() < expected) {
            throw new IllegalStateException("Некорректная строка данных: " + line);
        }
        return parts.toArray(String[]::new);
    }

    private static String sanitizeFileName(String login) {
        return login.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case '|', '\\' -> result.append(next);
                    default -> {
                        result.append('\\');
                        result.append(next);
                    }
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
