package ru.fintrack.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.TransactionType;
import ru.fintrack.model.Wallet;
import ru.fintrack.validation.InputValidator;

/**
 * Подсчёт доходов/расходов и формирование текстового отчёта.
 */
public class StatisticsService {

    private final FinanceService financeService;

    public StatisticsService(FinanceService financeService) {
        this.financeService = financeService;
    }

    public String buildFullReport() {
        Wallet wallet = financeService.getWallet();
        StringBuilder report = new StringBuilder();

        report.append("Общий доход: ").append(formatAmount(wallet.getTotalIncome())).append('\n');
        report.append("Доходы по категориям:\n");
        appendCategoryTotals(report, wallet.getTotalsByCategory(TransactionType.INCOME));

        report.append("Общие расходы: ").append(formatAmount(wallet.getTotalExpense())).append('\n');
        report.append("Расходы по категориям:\n");
        appendCategoryTotals(report, wallet.getTotalsByCategory(TransactionType.EXPENSE));

        report.append("Бюджет по категориям:\n");
        Map<Category, Budget> budgets = wallet.getBudgets();
        if (budgets.isEmpty()) {
            report.append("(не заданы)\n");
        } else {
            for (Budget budget : budgets.values()) {
                Double remaining = wallet.getRemainingBudget(budget.getCategory());
                report.append(budget.getCategory().getName())
                        .append(": ").append(formatAmount(budget.getLimit()))
                        .append(", Оставшийся бюджет: ").append(formatAmount(remaining))
                        .append('\n');
            }
        }

        List<String> alerts = financeService.checkBudgetAlerts();
        if (!alerts.isEmpty()) {
            report.append("Оповещения:\n");
            for (String alert : alerts) {
                report.append("- ").append(alert).append('\n');
            }
        }

        return report.toString().stripTrailing();
    }

    public CategoryStatsResult calculateForCategories(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            throw new IllegalArgumentException("Укажите хотя бы одну категорию.");
        }

        Wallet wallet = financeService.getWallet();
        Set<Category> knownCategories = collectKnownCategories(wallet);
        CategoryStatsResult result = new CategoryStatsResult();

        for (String rawName : categoryNames) {
            InputValidator.requireNonBlank(rawName, "категория");
            String name = rawName.trim();
            Category category = new Category(name);

            if (!knownCategories.contains(category)) {
                result.addMissingCategory(name);
                continue;
            }

            double income = wallet.getIncomeTotal(category);
            double expense = wallet.getExpenseTotal(category);
            if (income > 0) {
                result.putIncome(category.getName(), income);
            }
            if (expense > 0) {
                result.putExpense(category.getName(), expense);
            }
        }

        return result;
    }

    public String buildCategoryReport(List<String> categoryNames) {
        CategoryStatsResult stats = calculateForCategories(categoryNames);
        StringBuilder report = new StringBuilder();

        report.append("Подсчёт по выбранным категориям:\n");
        report.append("Сумма доходов: ").append(formatAmount(stats.getSelectedIncomeTotal())).append('\n');
        if (!stats.getIncomeByCategory().isEmpty()) {
            report.append("Доходы:\n");
            for (Map.Entry<String, Double> entry : stats.getIncomeByCategory().entrySet()) {
                report.append(entry.getKey()).append(": ").append(formatAmount(entry.getValue())).append('\n');
            }
        }

        report.append("Сумма расходов: ").append(formatAmount(stats.getSelectedExpenseTotal())).append('\n');
        if (!stats.getExpenseByCategory().isEmpty()) {
            report.append("Расходы:\n");
            for (Map.Entry<String, Double> entry : stats.getExpenseByCategory().entrySet()) {
                report.append(entry.getKey()).append(": ").append(formatAmount(entry.getValue())).append('\n');
            }
        }

        if (!stats.getMissingCategories().isEmpty()) {
            report.append("Категории не найдены: ")
                    .append(String.join(", ", stats.getMissingCategories()))
                    .append('\n');
        }

        return report.toString().stripTrailing();
    }

    private static Set<Category> collectKnownCategories(Wallet wallet) {
        Set<Category> known = new LinkedHashSet<>();
        for (Transaction transaction : wallet.getTransactions()) {
            known.add(transaction.getCategory());
        }
        known.addAll(wallet.getBudgets().keySet());
        return known;
    }

    private static void appendCategoryTotals(StringBuilder report, Map<Category, Double> totals) {
        if (totals.isEmpty()) {
            report.append("(нет данных)\n");
            return;
        }
        for (Map.Entry<Category, Double> entry : totals.entrySet()) {
            report.append(entry.getKey().getName())
                    .append(": ")
                    .append(formatAmount(entry.getValue()))
                    .append('\n');
        }
    }

    private static String formatAmount(double amount) {
        return String.valueOf(amount);
    }
}
