package ru.fintrack.service;

import java.util.ArrayList;
import java.util.List;

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
        return buildExpenseAlerts(wallet, category);
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

    public double getBalance() {
        return currentWallet().getBalance();
    }

    public Wallet getWallet() {
        return currentWallet();
    }

    public List<String> checkBudgetAlerts() {
        Wallet wallet = currentWallet();
        List<String> alerts = new ArrayList<>();
        for (Budget budget : wallet.getBudgets().values()) {
            double spent = wallet.getExpenseTotal(budget.getCategory());
            double remaining = budget.getLimit() - spent;
            if (remaining < 0) {
                alerts.add(formatBudgetExceeded(budget.getCategory().getName(), budget.getLimit(), spent, remaining));
            }
        }
        if (wallet.getTotalExpense() > wallet.getTotalIncome()) {
            alerts.add(formatExpensesExceedIncome(wallet.getTotalIncome(), wallet.getTotalExpense()));
        }
        return alerts;
    }

    private List<String> buildExpenseAlerts(Wallet wallet, Category category) {
        List<String> alerts = new ArrayList<>();

        Budget budget = wallet.getBudget(category);
        if (budget != null) {
            double spent = wallet.getExpenseTotal(category);
            double remaining = budget.getLimit() - spent;
            if (remaining < 0) {
                alerts.add(formatBudgetExceeded(category.getName(), budget.getLimit(), spent, remaining));
            }
        }

        if (wallet.getTotalExpense() > wallet.getTotalIncome()) {
            alerts.add(formatExpensesExceedIncome(wallet.getTotalIncome(), wallet.getTotalExpense()));
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

    private static String formatBudgetExceeded(String category, double limit, double spent, double remaining) {
        return "Превышен бюджет по категории \"" + category + "\": лимит "
                + limit + ", потрачено " + spent + ", остаток " + remaining + ".";
    }

    private static String formatExpensesExceedIncome(double income, double expense) {
        return "Расходы (" + expense + ") превысили доходы (" + income + ").";
    }
}
