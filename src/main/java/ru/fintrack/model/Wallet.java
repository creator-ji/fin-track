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

    public boolean removeBudget(Category category) {
        return budgets.remove(category) != null;
    }

    public Map<Category, Budget> getBudgets() {
        return Collections.unmodifiableMap(budgets);
    }

    /**
     * Переименовывает категорию во всех операциях и бюджетах.
     *
     * @return число затронутых операций
     */
    public int renameCategory(Category oldCategory, Category newCategory) {
        if (oldCategory.equals(newCategory)) {
            return 0;
        }
        if (!containsCategory(oldCategory)) {
            throw new IllegalArgumentException("Категория \"" + oldCategory.getName() + "\" не найдена.");
        }
        if (containsCategory(newCategory) && !oldCategory.equals(newCategory)) {
            throw new IllegalArgumentException("Категория \"" + newCategory.getName() + "\" уже существует.");
        }

        List<Transaction> updated = new ArrayList<>();
        int changed = 0;
        for (Transaction transaction : transactions) {
            if (transaction.getCategory().equals(oldCategory)) {
                updated.add(new Transaction(
                        transaction.getType(),
                        newCategory,
                        transaction.getAmount(),
                        transaction.getComment()
                ));
                changed++;
            } else {
                updated.add(transaction);
            }
        }
        transactions.clear();
        transactions.addAll(updated);

        Budget oldBudget = budgets.remove(oldCategory);
        if (oldBudget != null) {
            budgets.put(newCategory, new Budget(newCategory, oldBudget.getLimit()));
        }
        return changed;
    }

    public boolean containsCategory(Category category) {
        if (budgets.containsKey(category)) {
            return true;
        }
        for (Transaction transaction : transactions) {
            if (transaction.getCategory().equals(category)) {
                return true;
            }
        }
        return false;
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
