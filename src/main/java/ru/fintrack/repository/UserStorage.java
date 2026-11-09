package ru.fintrack.repository;

import java.util.Optional;

import ru.fintrack.model.User;
import ru.fintrack.model.Wallet;

/**
 * Персистентное хранилище пользователя и кошелька.
 */
public interface UserStorage {

    void save(User user);

    Optional<User> load(String login);

    void loadWalletInto(User user);

    static void copyWallet(Wallet source, Wallet target) {
        target.clear();
        source.getTransactions().forEach(target::addTransaction);
        source.getBudgets().values().forEach(target::setBudget);
    }
}
