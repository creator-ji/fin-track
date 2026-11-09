package ru.fintrack.ui;

import java.util.List;
import java.util.Map;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Wallet;

/**
 * Форматированный вывод таблиц для CLI.
 */
public final class ConsoleFormatter {

    private ConsoleFormatter() {
    }

    public static String formatBudgetTable(Wallet wallet) {
        Map<Category, Budget> budgets = wallet.getBudgets();
        if (budgets.isEmpty()) {
            return "Бюджеты не заданы.";
        }
        StringBuilder table = new StringBuilder();
        table.append(String.format("%-24s %12s %12s %12s%n", "Категория", "Лимит", "Потрачено", "Остаток"));
        table.append("-".repeat(64)).append('\n');
        for (Budget budget : budgets.values()) {
            Category category = budget.getCategory();
            double spent = wallet.getExpenseTotal(category);
            double remaining = budget.getLimit() - spent;
            table.append(String.format("%-24s %12.1f %12.1f %12.1f%n",
                    truncate(category.getName(), 24),
                    budget.getLimit(),
                    spent,
                    remaining));
        }
        return table.toString().stripTrailing();
    }

    public static String formatBalanceSummary(Wallet wallet) {
        return String.format("""
                +------------------+---------------+
                | Показатель       | Сумма         |
                +------------------+---------------+
                | Баланс           | %13.1f |
                | Доходы           | %13.1f |
                | Расходы          | %13.1f |
                +------------------+---------------+
                """.stripTrailing(),
                wallet.getBalance(),
                wallet.getTotalIncome(),
                wallet.getTotalExpense());
    }

    public static void printAlerts(List<String> alerts) {
        for (String alert : alerts) {
            System.out.println("Внимание: " + alert);
        }
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
