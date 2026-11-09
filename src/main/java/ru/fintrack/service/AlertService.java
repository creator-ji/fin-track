package ru.fintrack.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Wallet;

/**
 * Формирование оповещений по состоянию кошелька (SRP).
 */
public class AlertService {

    public static final double BUDGET_WARNING_RATIO = 0.8;

    public List<String> analyze(Wallet wallet) {
        List<String> alerts = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Budget budget : wallet.getBudgets().values()) {
            double spent = wallet.getExpenseTotal(budget.getCategory());
            double limit = budget.getLimit();
            double remaining = limit - spent;
            String categoryName = budget.getCategory().getName();

            if (remaining < 0) {
                addUnique(alerts, seen, "Превышен бюджет по категории \"" + categoryName
                        + "\": лимит " + limit + ", потрачено " + spent
                        + ", остаток " + remaining + ".");
            } else if (limit > 0 && spent >= limit * BUDGET_WARNING_RATIO) {
                double percent = spent / limit * 100.0;
                addUnique(alerts, seen, "Использовано " + String.format("%.0f", percent)
                        + "% бюджета по категории \"" + categoryName + "\" (потрачено "
                        + spent + " из " + limit + ").");
            }
        }

        if (wallet.getTotalExpense() > wallet.getTotalIncome()) {
            addUnique(alerts, seen, "Расходы (" + wallet.getTotalExpense()
                    + ") превысили доходы (" + wallet.getTotalIncome() + ").");
        }

        double balance = wallet.getBalance();
        if (balance < 0) {
            addUnique(alerts, seen, "Отрицательный баланс кошелька: " + balance + ".");
        } else if (balance == 0.0 && !wallet.getTransactions().isEmpty()) {
            addUnique(alerts, seen, "Баланс кошелька равен нулю.");
        }

        return alerts;
    }

    private static void addUnique(List<String> alerts, Set<String> seen, String alert) {
        if (seen.add(alert)) {
            alerts.add(alert);
        }
    }
}
