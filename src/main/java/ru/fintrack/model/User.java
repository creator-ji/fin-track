package ru.fintrack.model;

/**
 * Пользователь системы с привязанным кошельком.
 */
public class User {
    private final String login;
    private final String password;
    private final Wallet wallet;

    public User(String login, String password) {
        this.login = login;
        this.password = password;
        this.wallet = new Wallet();
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public boolean matchesPassword(String candidate) {
        return password.equals(candidate);
    }

    @Override
    public String toString() {
        return "User{login='" + login + "'}";
    }
}
