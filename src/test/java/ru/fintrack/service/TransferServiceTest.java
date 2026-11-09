package ru.fintrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.repository.InMemoryUserRepository;
import ru.fintrack.repository.UserRepository;

class TransferServiceTest {

    @TempDir
    Path tempDir;

    private AuthService authService;
    private FinanceService financeService;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        UserRepository repository = new InMemoryUserRepository();
        WalletFileStorage storage = new WalletFileStorage(tempDir);
        authService = new AuthService(repository, storage);
        financeService = new FinanceService(authService);
        transferService = new TransferService(authService, storage);

        authService.register("alice", "secret");
        authService.register("bob", "secret");
        authService.login("alice", "secret");
        financeService.addIncome("Зарплата", 1000, null);
    }

    @Test
    void transferMovesMoneyBetweenWallets() {
        transferService.transfer("bob", 300);
        assertEquals(700.0, financeService.getBalance());

        authService.login("bob", "secret");
        assertEquals(300.0, financeService.getBalance());
    }

    @Test
    void cannotTransferToSelf() {
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer("alice", 10));
    }
}
