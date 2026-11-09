package ru.fintrack.service;

import java.util.ArrayList;
import java.util.List;

import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.TransactionType;
import ru.fintrack.model.User;
import ru.fintrack.repository.UserRepository;
import ru.fintrack.validation.InputValidator;

/**
 * Переводы между кошельками пользователей по логину.
 */
public class TransferService {

    public static final String TRANSFER_CATEGORY = "Перевод";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final WalletFileStorage walletFileStorage;

    public TransferService(
            AuthService authService,
            UserRepository userRepository,
            WalletFileStorage walletFileStorage
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.walletFileStorage = walletFileStorage;
    }

    /**
     * Переводит сумму от текущего пользователя получателю.
     * У отправителя фиксируется расход, у получателя — доход.
     *
     * @return оповещения (например, расходы превысили доходы)
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

        User recipient = resolveUser(normalizedRecipient);
        walletFileStorage.loadWalletInto(recipient);

        Category category = new Category(TRANSFER_CATEGORY);
        sender.getWallet().addTransaction(new Transaction(
                TransactionType.EXPENSE,
                category,
                amount,
                "перевод пользователю " + recipient.getLogin()
        ));
        recipient.getWallet().addTransaction(new Transaction(
                TransactionType.INCOME,
                category,
                amount,
                "перевод от пользователя " + sender.getLogin()
        ));

        userRepository.save(recipient);
        authService.saveUser(sender);
        authService.saveUser(recipient);

        List<String> alerts = new ArrayList<>();
        if (sender.getWallet().getTotalExpense() > sender.getWallet().getTotalIncome()) {
            alerts.add("Расходы (" + sender.getWallet().getTotalExpense()
                    + ") превысили доходы (" + sender.getWallet().getTotalIncome() + ").");
        }
        return alerts;
    }

    private User resolveUser(String login) {
        return userRepository.findByLogin(login)
                .or(() -> walletFileStorage.load(login))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Получатель с логином \"" + login + "\" не найден."));
    }
}
