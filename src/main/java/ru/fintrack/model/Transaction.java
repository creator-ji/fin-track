package ru.fintrack.model;

/**
 * Финансовая операция: доход или расход.
 */
public class Transaction {
    private final TransactionType type;
    private final Category category;
    private final double amount;
    private final String comment;

    public Transaction(TransactionType type, Category category, double amount) {
        this(type, category, amount, null);
    }

    public Transaction(TransactionType type, Category category, double amount, String comment) {
        if (type == null) {
            throw new IllegalArgumentException("Тип операции обязателен.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Категория обязательна.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.comment = comment;
    }

    public static Transaction income(Category category, double amount, String comment) {
        return new Transaction(TransactionType.INCOME, category, amount, comment);
    }

    public static Transaction expense(Category category, double amount, String comment) {
        return new Transaction(TransactionType.EXPENSE, category, amount, comment);
    }

    public TransactionType getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getComment() {
        return comment;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    @Override
    public String toString() {
        String base = type + " | " + category.getName() + ": " + amount;
        if (comment != null && !comment.isBlank()) {
            return base + " (" + comment + ")";
        }
        return base;
    }
}
