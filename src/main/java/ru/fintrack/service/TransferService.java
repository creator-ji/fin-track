package ru.fintrack.service;

import java.util.List;

import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.User;
import ru.fintrack.repository.UserStorage;
import ru.fintrack.validation.InputValidator;

/**
 * Переводы между кошельками пользователей по логину.
 */
public class TransferService {

    public static final String TRANSFER_CATEGORY = "Перевод";

    private final AuthService authService;
    private final UserStorage userStorage;
    private final AlertService alertService;

    public TransferService(AuthService authService, UserStorage userStorage) {
        this(authService, userStorage, new AlertService());
    }

    public TransferService(AuthService authService, UserStorage userStorage, AlertService alertService) {
        this.authService = authService;
        this.userStorage = userStorage;
        this.alertService = alertService;
    }

    /**
     * Переводит сумму от текущего пользователя получателю.
     * У отправителя фиксируется расход, у получателя — доход.
     */
    public List<String> transfer(String recipientLogin, double amount) {
        InputValidator.requireNonBlank(recipientLogin, "логин получателя");
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть больше нуля.");
        }

        User sender = authService.requireCurrentUser();
        String normalizedRecipient = recipientLogin.trim();

        if (sender.getLogin().equalsIgnoreCase(normalizedRecipient)) {
            throw new IllegalArgumentException("Нельзя выполнить перевод самому себе.");
        }

        User recipient;
        try {
            recipient = authService.findExistingUser(normalizedRecipient);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Получатель с логином \"" + normalizedRecipient + "\" не найден.");
        }
        userStorage.loadWalletInto(recipient);

        Category category = new Category(TRANSFER_CATEGORY);
        sender.getWallet().addTransaction(Transaction.expense(
                category,
                amount,
                "перевод пользователю " + recipient.getLogin()
        ));
        recipient.getWallet().addTransaction(Transaction.income(
                category,
                amount,
                "перевод от пользователя " + sender.getLogin()
        ));

        authService.persist(sender);
        authService.persist(recipient);
        return alertService.analyze(sender.getWallet());
    }
}
