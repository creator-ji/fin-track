package ru.fintrack.service;

import java.util.List;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.User;
import ru.fintrack.model.Wallet;
import ru.fintrack.validation.InputValidator;

/**
 * Управление доходами, расходами и бюджетами авторизованного пользователя.
 */
public class FinanceService {

    private final AuthService authService;
    private final AlertService alertService;

    public FinanceService(AuthService authService) {
        this(authService, new AlertService());
    }

    public FinanceService(AuthService authService, AlertService alertService) {
        this.authService = authService;
        this.alertService = alertService;
    }

    public Transaction addIncome(String categoryName, double amount, String comment) {
        validateOperation(categoryName, amount);
        Transaction transaction = Transaction.income(new Category(categoryName.trim()), amount, comment);
        currentWallet().addTransaction(transaction);
        return transaction;
    }

    public List<String> addExpense(String categoryName, double amount, String comment) {
        validateOperation(categoryName, amount);
        Wallet wallet = currentWallet();
        wallet.addTransaction(Transaction.expense(new Category(categoryName.trim()), amount, comment));
        return alertService.analyze(wallet);
    }

    public Budget setBudget(String categoryName, double limit) {
        Category category = requireCategory(categoryName);
        requireNonNegativeLimit(limit);
        Budget budget = new Budget(category, limit);
        currentWallet().setBudget(budget);
        return budget;
    }

    public Budget updateBudget(String categoryName, double newLimit) {
        Category category = requireCategory(categoryName);
        requireNonNegativeLimit(newLimit);
        Budget existing = currentWallet().getBudget(category);
        if (existing == null) {
            throw new IllegalArgumentException("Бюджет для категории \"" + category.getName() + "\" не найден.");
        }
        existing.setLimit(newLimit);
        return existing;
    }

    public void removeBudget(String categoryName) {
        Category category = requireCategory(categoryName);
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
        return alertService.analyze(currentWallet());
    }

    private Wallet currentWallet() {
        User user = authService.requireCurrentUser();
        return user.getWallet();
    }

    private static Category requireCategory(String categoryName) {
        InputValidator.requireNonBlank(categoryName, "категория");
        return new Category(categoryName.trim());
    }

    private static void requireNonNegativeLimit(double limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Лимит бюджета не может быть отрицательным.");
        }
    }

    private static void validateOperation(String categoryName, double amount) {
        InputValidator.requireNonBlank(categoryName, "категория");
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }
    }
}
