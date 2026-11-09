package ru.fintrack.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import ru.fintrack.model.User;

/**
 * In-memory хранилище пользователей.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersByLogin = new LinkedHashMap<>();

    @Override
    public void save(User user) {
        usersByLogin.put(normalize(user.getLogin()), user);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByLogin.get(normalize(login)));
    }

    @Override
    public boolean existsByLogin(String login) {
        return findByLogin(login).isPresent();
    }

    @Override
    public Collection<User> findAll() {
        return Collections.unmodifiableCollection(usersByLogin.values());
    }

    private static String normalize(String login) {
        return login.trim().toLowerCase();
    }
}
