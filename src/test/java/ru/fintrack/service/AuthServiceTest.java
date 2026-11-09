package ru.fintrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.TransactionType;
import ru.fintrack.model.User;
import ru.fintrack.repository.InMemoryUserRepository;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(new InMemoryUserRepository(), new WalletFileStorage(tempDir));
    }

    @Test
    void registerAndLoginSuccess() {
        authService.register("alice", "secret");
        User user = authService.login("alice", "secret");
        assertEquals("alice", user.getLogin());
        assertTrue(authService.isAuthenticated());
    }

    @Test
    void duplicateRegisterFails() {
        authService.register("alice", "secret");
        assertThrows(IllegalArgumentException.class, () -> authService.register("alice", "other"));
    }

    @Test
    void wrongPasswordFails() {
        authService.register("alice", "secret");
        assertThrows(IllegalArgumentException.class, () -> authService.login("alice", "bad"));
    }

    @Test
    void persistsWalletAcrossRelogin() {
        authService.register("alice", "secret");
        authService.login("alice", "secret");
        authService.requireCurrentUser().getWallet()
                .addTransaction(new Transaction(TransactionType.INCOME, new Category("Зарплата"), 500));
        authService.logout();

        AuthService fresh = new AuthService(new InMemoryUserRepository(), new WalletFileStorage(tempDir));
        User loaded = fresh.login("alice", "secret");
        assertEquals(500.0, loaded.getWallet().getBalance());
    }
}
