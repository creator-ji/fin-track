package ru.fintrack.ui;

import java.nio.file.Path;
import java.util.Scanner;

import ru.fintrack.io.DataExchangeService;
import ru.fintrack.io.ReportWriter;
import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.repository.InMemoryUserRepository;
import ru.fintrack.repository.UserRepository;
import ru.fintrack.repository.UserStorage;
import ru.fintrack.service.AlertService;
import ru.fintrack.service.AuthService;
import ru.fintrack.service.FinanceService;
import ru.fintrack.service.StatisticsService;
import ru.fintrack.service.TransferService;

/**
 * Консольное приложение: цикл команд и композиция зависимостей.
 */
public class ConsoleApplication {

    private final ConsoleSession session;
    private final CommandRegistry registry;
    private final Scanner scanner;

    public ConsoleApplication(ConsoleSession session, CommandRegistry registry, Scanner scanner) {
        this.session = session;
        this.registry = registry;
        this.scanner = scanner;
    }

    public static ConsoleApplication createDefault() {
        Path dataDir = Path.of("data");
        UserStorage userStorage = new WalletFileStorage(dataDir);
        UserRepository userRepository = new InMemoryUserRepository();
        AlertService alertService = new AlertService();

        AuthService authService = new AuthService(userRepository, userStorage);
        FinanceService financeService = new FinanceService(authService, alertService);
        StatisticsService statisticsService = new StatisticsService(financeService);
        TransferService transferService = new TransferService(authService, userStorage, alertService);

        Scanner scanner = new Scanner(System.in);
        ConsoleSession session = new ConsoleSession(
                new ConsoleInput(scanner),
                authService,
                financeService,
                statisticsService,
                transferService,
                new ReportWriter(),
                new DataExchangeService()
        );
        return new ConsoleApplication(session, FinTrackCommands.create(), scanner);
    }

    public void run() {
        System.out.println("=== FinTrack ===");
        System.out.println("Введите help для списка команд и примеров.");

        boolean running = true;
        while (running) {
            System.out.print("> ");
            String raw = scanner.nextLine().trim().toLowerCase();
            if (raw.isEmpty()) {
                continue;
            }

            try {
                running = registry.find(raw)
                        .orElse(s -> {
                            System.out.println("Неизвестная команда. Введите help для списка команд.");
                            return true;
                        })
                        .execute(session);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
        scanner.close();
    }
}
