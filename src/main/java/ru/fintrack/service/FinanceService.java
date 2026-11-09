package ru.fintrack.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.TransactionType;
import ru.fintrack.model.User;
import ru.fintrack.model.Wallet;
import ru.fintrack.validation.InputValidator;

/**
 * Управление доходами, расходами и бюджетами авторизованного пользователя.
 */
public class FinanceService {

    public static final double BUDGET_WARNING_RATIO = 0.8;

    private final AuthService authService;

    public FinanceService(AuthService authService) {
        this.authService = authService;
    }

    public Transaction addIncome(String categoryName, double amount, String comment) {
        validateOperation(categoryName, amount);
        Wallet wallet = currentWallet();
        Category category = new Category(categoryName.trim());
        Transaction transaction = new Transaction(TransactionType.INCOME, category, amount, comment);
        wallet.addTransaction(transaction);
        return transaction;
    }

    public List<String> addExpense(String categoryName, double amount, String comment) {
        validateOperation(categoryName, amount);
        Wallet wallet = currentWallet();
        Category category = new Category(categoryName.trim());
        Transaction transaction = new Transaction(TransactionType.EXPENSE, category, amount, comment);
        wallet.addTransaction(transaction);
        return collectAlerts(wallet);
    }

    public Budget setBudget(String categoryName, double limit) {
        InputValidator.requireNonBlank(categoryName, "категория");
        if (limit < 0) {
            throw new IllegalArgumentException("Лимит бюджета не может быть отрицательным.");
        }

        Wallet wallet = currentWallet();
        Category category = new Category(categoryName.trim());
        Budget budget = new Budget(category, limit);
        wallet.setBudget(budget);
        return budget;
    }

    public Budget updateBudget(String categoryName, double newLimit) {
        InputValidator.requireNonBlank(categoryName, "категория");
        if (newLimit < 0) {
            throw new IllegalArgumentException("Лимит бюджета не может быть отрицательным.");
        }
        Wallet wallet = currentWallet();
        Category category = new Category(categoryName.trim());
        Budget existing = wallet.getBudget(category);
        if (existing == null) {
            throw new IllegalArgumentException("Бюджет для категории \"" + category.getName() + "\" не найден.");
        }
        existing.setLimit(newLimit);
        return existing;
    }

    public void removeBudget(String categoryName) {
        InputValidator.requireNonBlank(categoryName, "категория");
        Category category = new Category(categoryName.trim());
        if (!currentWallet().removeBudget(category)) {
            throw new IllegalArgumentException("Бюджет для категории \"" + category.getName() + "\" не найден.");
        }
    }

    public int renameCategory(String oldName, String newName) {
        InputValidator.requireNonBlank(oldName, "старое имя категории");
        InputValidator.requireNonBlank(newName, "новое имя категории");
        return currentWallet().renameCategory(new Category(oldName.trim()), new Category(newName.trim()));
    }

    public double getBalance() {
        return currentWallet().getBalance();
    }

    public Wallet getWallet() {
        return currentWallet();
    }

    public List<String> checkBudgetAlerts() {
        return collectAlerts(currentWallet());
    }

    private List<String> collectAlerts(Wallet wallet) {
        List<String> alerts = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Budget budget : wallet.getBudgets().values()) {
            double spent = wallet.getExpenseTotal(budget.getCategory());
            double limit = budget.getLimit();
            double remaining = limit - spent;
            String categoryName = budget.getCategory().getName();

            if (remaining < 0) {
                addUnique(alerts, seen, formatBudgetExceeded(categoryName, limit, spent, remaining));
            } else if (limit > 0 && spent >= limit * BUDGET_WARNING_RATIO) {
                double percent = spent / limit * 100.0;
                addUnique(alerts, seen, "Использовано " + String.format("%.0f", percent)
                        + "% бюджета по категории \"" + categoryName + "\" (потрачено "
                        + spent + " из " + limit + ").");
            }
        }

        if (wallet.getTotalExpense() > wallet.getTotalIncome()) {
            addUnique(alerts, seen, formatExpensesExceedIncome(wallet.getTotalIncome(), wallet.getTotalExpense()));
        }

        double balance = wallet.getBalance();
        if (balance < 0) {
            addUnique(alerts, seen, "Отрицательный баланс кошелька: " + balance + ".");
        } else if (balance == 0.0 && !wallet.getTransactions().isEmpty()) {
            addUnique(alerts, seen, "Баланс кошелька равен нулю.");
        }

        return alerts;
    }

    private Wallet currentWallet() {
        User user = authService.requireCurrentUser();
        return user.getWallet();
    }

    private static void validateOperation(String categoryName, double amount) {
        InputValidator.requireNonBlank(categoryName, "категория");
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }
    }

    private static void addUnique(List<String> alerts, Set<String> seen, String alert) {
        if (seen.add(alert)) {
            alerts.add(alert);
        }
    }

    private static String formatBudgetExceeded(String category, double limit, double spent, double remaining) {
        return "Превышен бюджет по категории \"" + category + "\": лимит "
                + limit + ", потрачено " + spent + ", остаток " + remaining + ".";
    }

    private static String formatExpensesExceedIncome(double income, double expense) {
        return "Расходы (" + expense + ") превысили доходы (" + income + ").";
    }
}
