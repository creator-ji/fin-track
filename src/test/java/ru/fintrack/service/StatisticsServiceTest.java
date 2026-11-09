package ru.fintrack.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.repository.InMemoryUserRepository;

class StatisticsServiceTest {

    @TempDir
    Path tempDir;

    private StatisticsService statisticsService;
    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        AuthService authService = new AuthService(new InMemoryUserRepository(), new WalletFileStorage(tempDir));
        authService.register("alice", "secret");
        authService.login("alice", "secret");
        financeService = new FinanceService(authService);
        statisticsService = new StatisticsService(financeService);
    }

    @Test
    void fullReportContainsTotalsAndBudgets() {
        financeService.addIncome("Зарплата", 20000, null);
        financeService.addExpense("Еда", 800, null);
        financeService.setBudget("Еда", 4000);
        String report = statisticsService.buildFullReport();
        assertTrue(report.contains("Общий доход: 20000.0"));
        assertTrue(report.contains("Общие расходы: 800.0"));
        assertTrue(report.contains("Оставшийся бюджет: 3200.0"));
    }

    @Test
    void categoryFilterReportsMissing() {
        financeService.addExpense("Еда", 100, null);
        String report = statisticsService.buildCategoryReport(List.of("Еда", "НетТакой"));
        assertTrue(report.contains("Еда: 100.0"));
        assertTrue(report.contains("Категории не найдены: НетТакой"));
    }
}
