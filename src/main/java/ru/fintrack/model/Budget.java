package ru.fintrack.model;

/**
 * Бюджет (лимит) по категории расходов.
 */
public class Budget {
    private final Category category;
    private double limit;

    public Budget(Category category, double limit) {
        this.category = category;
        this.limit = limit;
    }

    public Category getCategory() {
        return category;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return category.getName() + ": " + limit;
    }
}
