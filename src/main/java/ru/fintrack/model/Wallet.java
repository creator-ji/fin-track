package ru.fintrack.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Кошелёк пользователя: операции и бюджеты по категориям.
 */
public class Wallet {
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<Category, Budget> budgets = new HashMap<>();

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void setBudget(Budget budget) {
        budgets.put(budget.getCategory(), budget);
    }

    public Budget getBudget(Category category) {
        return budgets.get(category);
    }

    public Map<Category, Budget> getBudgets() {
        return Collections.unmodifiableMap(budgets);
    }

    /**
     * Текущий баланс: сумма доходов минус сумма расходов.
     */
    public double getBalance() {
        return getTotalIncome() - getTotalExpense();
    }

    public double getTotalIncome() {
        return sumByType(TransactionType.INCOME);
    }

    public double getTotalExpense() {
        return sumByType(TransactionType.EXPENSE);
    }

    public double getIncomeTotal(Category category) {
        return sumByTypeAndCategory(TransactionType.INCOME, category);
    }

    public double getExpenseTotal(Category category) {
        return sumByTypeAndCategory(TransactionType.EXPENSE, category);
    }

    /**
     * Остаток бюджета по категории: лимит минус расходы. {@code null}, если бюджет не задан.
     */
    public Double getRemainingBudget(Category category) {
        Budget budget = budgets.get(category);
        if (budget == null) {
            return null;
        }
        return budget.getLimit() - getExpenseTotal(category);
    }

    public Map<Category, Double> getTotalsByCategory(TransactionType type) {
        Map<Category, Double> totals = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            if (transaction.getType() != type) {
                continue;
            }
            Category category = transaction.getCategory();
            totals.merge(category, transaction.getAmount(), Double::sum);
        }
        return totals;
    }

    public void clear() {
        transactions.clear();
        budgets.clear();
    }

    private double sumByType(TransactionType type) {
        double sum = 0.0;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == type) {
                sum += transaction.getAmount();
            }
        }
        return sum;
    }

    private double sumByTypeAndCategory(TransactionType type, Category category) {
        double sum = 0.0;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == type && transaction.getCategory().equals(category)) {
                sum += transaction.getAmount();
            }
        }
        return sum;
    }
}
