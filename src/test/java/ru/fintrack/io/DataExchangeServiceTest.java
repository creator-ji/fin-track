package ru.fintrack.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.TransactionType;
import ru.fintrack.model.Wallet;

class DataExchangeServiceTest {

    @TempDir
    Path tempDir;

    private final DataExchangeService exchange = new DataExchangeService();

    @Test
    void csvExportImportRoundTrip() {
        Wallet source = sampleWallet();
        Path file = tempDir.resolve("wallet.csv");
        exchange.exportCsv(source, file);

        Wallet target = new Wallet();
        exchange.importCsv(target, file);
        assertEquals(source.getBalance(), target.getBalance());
        assertEquals(1, target.getBudgets().size());
        assertEquals(2, target.getTransactions().size());
    }

    @Test
    void jsonExportImportRoundTrip() {
        Wallet source = sampleWallet();
        Path file = tempDir.resolve("wallet.json");
        exchange.exportJson(source, file);

        Wallet target = new Wallet();
        exchange.importJson(target, file);
        assertEquals(source.getTotalIncome(), target.getTotalIncome());
        assertEquals(source.getTotalExpense(), target.getTotalExpense());
        assertEquals(4000.0, target.getBudget(new Category("Еда")).getLimit());
    }

    private static Wallet sampleWallet() {
        Wallet wallet = new Wallet();
        wallet.addTransaction(new Transaction(TransactionType.INCOME, new Category("Зарплата"), 20000));
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, new Category("Еда"), 800));
        wallet.setBudget(new Budget(new Category("Еда"), 4000));
        return wallet;
    }
}
