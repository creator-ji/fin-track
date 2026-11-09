package ru.fintrack.repository;

import java.util.Collection;
import java.util.Optional;

import ru.fintrack.model.User;

/**
 * Хранилище пользователей.
 */
public interface UserRepository {

    void save(User user);

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    Collection<User> findAll();
}
