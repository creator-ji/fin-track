package ru.fintrack.service;

import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.model.User;
import ru.fintrack.repository.UserRepository;
import ru.fintrack.validation.InputValidator;

/**
 * Регистрация и авторизация пользователей с загрузкой/сохранением кошелька.
 */
public class AuthService {

    private final UserRepository userRepository;
    private final WalletFileStorage walletFileStorage;
    private User currentUser;

    public AuthService(UserRepository userRepository, WalletFileStorage walletFileStorage) {
        this.userRepository = userRepository;
        this.walletFileStorage = walletFileStorage;
    }

    public User register(String login, String password) {
        InputValidator.requireNonBlank(login, "логин");
        InputValidator.requireNonBlank(password, "пароль");

        String normalizedLogin = login.trim();
        if (userRepository.existsByLogin(normalizedLogin) || walletFileStorage.load(normalizedLogin).isPresent()) {
            throw new IllegalArgumentException("Пользователь с логином \"" + normalizedLogin + "\" уже существует.");
        }

        User user = new User(normalizedLogin, password);
        userRepository.save(user);
        walletFileStorage.save(user);
        return user;
    }

    public User login(String login, String password) {
        InputValidator.requireNonBlank(login, "логин");
        InputValidator.requireNonBlank(password, "пароль");

        if (currentUser != null) {
            walletFileStorage.save(currentUser);
        }

        String normalizedLogin = login.trim();
        User user = userRepository.findByLogin(normalizedLogin)
                .or(() -> walletFileStorage.load(normalizedLogin))
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        if (!user.matchesPassword(password)) {
            throw new IllegalArgumentException("Неверный пароль.");
        }

        walletFileStorage.loadWalletInto(user);
        userRepository.save(user);
        currentUser = user;
        return user;
    }

    public void logout() {
        if (currentUser != null) {
            walletFileStorage.save(currentUser);
            currentUser = null;
        }
    }

    /**
     * Сохраняет данные текущего пользователя (вызывать при выходе из приложения).
     */
    public void saveCurrentUser() {
        if (currentUser != null) {
            walletFileStorage.save(currentUser);
        }
    }

    public void saveUser(User user) {
        walletFileStorage.save(user);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User requireCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("Необходима авторизация.");
        }
        return currentUser;
    }
}
