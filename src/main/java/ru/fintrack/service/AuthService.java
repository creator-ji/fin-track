package ru.fintrack.service;

import ru.fintrack.model.User;
import ru.fintrack.repository.UserRepository;
import ru.fintrack.repository.UserStorage;
import ru.fintrack.validation.InputValidator;

/**
 * Регистрация и авторизация пользователей с загрузкой/сохранением кошелька.
 */
public class AuthService {

    private final UserRepository userRepository;
    private final UserStorage userStorage;
    private User currentUser;

    public AuthService(UserRepository userRepository, UserStorage userStorage) {
        this.userRepository = userRepository;
        this.userStorage = userStorage;
    }

    public User register(String login, String password) {
        InputValidator.requireNonBlank(login, "логин");
        InputValidator.requireNonBlank(password, "пароль");

        String normalizedLogin = login.trim();
        if (userRepository.existsByLogin(normalizedLogin) || userStorage.load(normalizedLogin).isPresent()) {
            throw new IllegalArgumentException("Пользователь с логином \"" + normalizedLogin + "\" уже существует.");
        }

        User user = new User(normalizedLogin, password);
        persist(user);
        return user;
    }

    public User login(String login, String password) {
        InputValidator.requireNonBlank(login, "логин");
        InputValidator.requireNonBlank(password, "пароль");

        if (currentUser != null) {
            userStorage.save(currentUser);
        }

        String normalizedLogin = login.trim();
        User user = findExistingUser(normalizedLogin);

        if (!user.matchesPassword(password)) {
            throw new IllegalArgumentException("Неверный пароль.");
        }

        userStorage.loadWalletInto(user);
        persist(user);
        currentUser = user;
        return user;
    }

    public void logout() {
        if (currentUser != null) {
            userStorage.save(currentUser);
            currentUser = null;
        }
    }

    public void saveCurrentUser() {
        if (currentUser != null) {
            userStorage.save(currentUser);
        }
    }

    public void persist(User user) {
        userRepository.save(user);
        userStorage.save(user);
    }

    /**
     * Ищет пользователя в памяти или на диске.
     */
    public User findExistingUser(String login) {
        InputValidator.requireNonBlank(login, "логин");
        String normalized = login.trim();
        return userRepository.findByLogin(normalized)
                .or(() -> userStorage.load(normalized))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Пользователь с логином \"" + normalized + "\" не найден."));
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
