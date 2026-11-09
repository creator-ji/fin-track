package ru.fintrack.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Результат подсчёта по выбранным категориям.
 */
public class CategoryStatsResult {

    private final Map<String, Double> incomeByCategory = new LinkedHashMap<>();
    private final Map<String, Double> expenseByCategory = new LinkedHashMap<>();
    private final List<String> missingCategories = new ArrayList<>();

    public void putIncome(String category, double amount) {
        incomeByCategory.put(category, amount);
    }

    public void putExpense(String category, double amount) {
        expenseByCategory.put(category, amount);
    }

    public void addMissingCategory(String category) {
        missingCategories.add(category);
    }

    public Map<String, Double> getIncomeByCategory() {
        return Collections.unmodifiableMap(incomeByCategory);
    }

    public Map<String, Double> getExpenseByCategory() {
        return Collections.unmodifiableMap(expenseByCategory);
    }

    public List<String> getMissingCategories() {
        return Collections.unmodifiableList(missingCategories);
    }

    public double getSelectedIncomeTotal() {
        return incomeByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getSelectedExpenseTotal() {
        return expenseByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
