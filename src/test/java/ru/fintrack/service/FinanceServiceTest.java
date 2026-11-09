package ru.fintrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.repository.InMemoryUserRepository;

class FinanceServiceTest {

    @TempDir
    Path tempDir;

    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        AuthService authService = new AuthService(new InMemoryUserRepository(), new WalletFileStorage(tempDir));
        authService.register("alice", "secret");
        authService.login("alice", "secret");
        financeService = new FinanceService(authService);
    }

    @Test
    void addsIncomeAndExpense() {
        financeService.addIncome("Зарплата", 1000, null);
        financeService.addExpense("Еда", 200, null);
        assertEquals(800.0, financeService.getBalance());
    }

    @Test
    void warnsWhenBudgetAlmostUsed() {
        financeService.setBudget("Еда", 100);
        List<String> alerts = financeService.addExpense("Еда", 80, null);
        assertTrue(alerts.stream().anyMatch(a -> a.contains("80%")));
    }

    @Test
    void notifiesWhenBudgetExceeded() {
        financeService.setBudget("Еда", 100);
        List<String> alerts = financeService.addExpense("Еда", 150, null);
        assertTrue(alerts.stream().anyMatch(a -> a.contains("Превышен бюджет")));
    }

    @Test
    void editAndRemoveBudget() {
        financeService.setBudget("Еда", 100);
        assertEquals(250.0, financeService.updateBudget("Еда", 250).getLimit());
        financeService.removeBudget("Еда");
        assertTrue(financeService.getWallet().getBudgets().isEmpty());
    }

    @Test
    void renameCategoryWorks() {
        financeService.addExpense("Еда", 10, null);
        financeService.setBudget("Еда", 100);
        int changed = financeService.renameCategory("Еда", "Продукты");
        assertEquals(1, changed);
        assertFalse(financeService.getWallet().containsCategory(new ru.fintrack.model.Category("Еда")));
    }

    @Test
    void rejectsInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () -> financeService.addIncome("A", 0, null));
    }
}
