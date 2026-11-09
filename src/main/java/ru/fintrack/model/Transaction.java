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
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.comment = comment;
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

    @Override
    public String toString() {
        String base = type + " | " + category.getName() + ": " + amount;
        if (comment != null && !comment.isBlank()) {
            return base + " (" + comment + ")";
        }
        return base;
    }
}
