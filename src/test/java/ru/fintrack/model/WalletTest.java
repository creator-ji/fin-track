package ru.fintrack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WalletTest {

    @Test
    void calculatesBalanceIncomeAndExpense() {
        Wallet wallet = new Wallet();
        wallet.addTransaction(new Transaction(TransactionType.INCOME, new Category("Зарплата"), 1000));
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, new Category("Еда"), 200));
        assertEquals(800.0, wallet.getBalance());
        assertEquals(1000.0, wallet.getTotalIncome());
        assertEquals(200.0, wallet.getTotalExpense());
    }

    @Test
    void remainingBudgetCanBeNegative() {
        Wallet wallet = new Wallet();
        Category food = new Category("Еда");
        wallet.setBudget(new Budget(food, 100));
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, food, 150));
        assertEquals(-50.0, wallet.getRemainingBudget(food));
    }

    @Test
    void renameCategoryUpdatesTransactionsAndBudget() {
        Wallet wallet = new Wallet();
        Category oldName = new Category("Еда");
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, oldName, 50));
        wallet.setBudget(new Budget(oldName, 200));

        int changed = wallet.renameCategory(oldName, new Category("Продукты"));
        assertEquals(1, changed);
        assertEquals(50.0, wallet.getExpenseTotal(new Category("Продукты")));
        assertEquals(200.0, wallet.getBudget(new Category("Продукты")).getLimit());
        assertTrue(wallet.getBudget(oldName) == null);
    }

    @Test
    void renameMissingCategoryFails() {
        Wallet wallet = new Wallet();
        assertThrows(IllegalArgumentException.class,
                () -> wallet.renameCategory(new Category("A"), new Category("B")));
    }
}
