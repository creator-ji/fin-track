package ru.fintrack.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Реестр CLI-команд.
 */
public class CommandRegistry {

    private final Map<String, Command> commands = new LinkedHashMap<>();

    public void register(String name, Command command) {
        commands.put(name.toLowerCase(), command);
    }

    public void registerAliases(Command command, String... names) {
        for (String name : names) {
            register(name, command);
        }
    }

    public Optional<Command> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(commands.get(name.trim().toLowerCase()));
    }

    public Set<String> names() {
        return commands.keySet();
    }

    public String shortList() {
        return String.join(" | ", names());
    }
}
