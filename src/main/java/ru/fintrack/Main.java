package ru.fintrack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import ru.fintrack.io.ReportWriter;
import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.model.Budget;
import ru.fintrack.model.Category;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.User;
import ru.fintrack.model.Wallet;
import ru.fintrack.repository.InMemoryUserRepository;
import ru.fintrack.repository.UserRepository;
import ru.fintrack.service.AuthService;
import ru.fintrack.service.FinanceService;
import ru.fintrack.service.StatisticsService;
import ru.fintrack.service.TransferService;
import ru.fintrack.validation.InputValidator;

/**
 * Точка входа: цикл команд авторизации, финансов, статистики и переводов.
 */
public class Main {

    private static final String COMMANDS = String.join(
            " | ",
            "register", "login", "whoami", "logout",
            "income", "expense", "budget", "balance", "budgets",
            "stats", "categories", "report", "transfer",
            "help", "exit"
    );

    public static void main(String[] args) {
        WalletFileStorage walletFileStorage = new WalletFileStorage(Path.of("data"));
        UserRepository userRepository = new InMemoryUserRepository();
        AuthService authService = new AuthService(userRepository, walletFileStorage);
        FinanceService financeService = new FinanceService(authService);
        StatisticsService statisticsService = new StatisticsService(financeService);
        TransferService transferService = new TransferService(authService, userRepository, walletFileStorage);
        ReportWriter reportWriter = new ReportWriter();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== FinTrack ===");
        System.out.println("Команды: " + COMMANDS);

        boolean running = true;
        while (running) {
            System.out.print("> ");
            String command = scanner.nextLine().trim().toLowerCase();

            try {
                switch (command) {
                    case "register" -> handleRegister(scanner, authService);
                    case "login" -> handleLogin(scanner, authService);
                    case "whoami" -> handleWhoAmI(authService);
                    case "logout" -> handleLogout(authService);
                    case "income" -> handleIncome(scanner, financeService);
                    case "expense" -> handleExpense(scanner, financeService);
                    case "budget" -> handleBudget(scanner, financeService);
                    case "balance" -> handleBalance(financeService);
                    case "budgets" -> handleBudgets(financeService);
                    case "stats" -> handleStats(statisticsService, reportWriter);
                    case "categories" -> handleCategories(scanner, statisticsService, reportWriter);
                    case "report" -> handleReport(scanner, authService, statisticsService, reportWriter);
                    case "transfer" -> handleTransfer(scanner, financeService, transferService);
                    case "help" -> printHelp();
                    case "exit", "quit" -> {
                        authService.saveCurrentUser();
                        System.out.println("Данные сохранены. Выход из приложения.");
                        running = false;
                    }
                    case "" -> {
                        // игнорируем пустую строку
                    }
                    default -> System.out.println("Неизвестная команда. Введите help для списка команд.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void handleRegister(Scanner scanner, AuthService authService) {
        System.out.print("Логин: ");
        String login = scanner.nextLine();
        System.out.print("Пароль: ");
        String password = scanner.nextLine();

        User user = authService.register(login, password);
        System.out.println("Пользователь \"" + user.getLogin() + "\" зарегистрирован.");
        System.out.println("Файл кошелька: data/" + user.getLogin().toLowerCase() + ".wallet");
    }

    private static void handleLogin(Scanner scanner, AuthService authService) {
        System.out.print("Логин: ");
        String login = scanner.nextLine();
        System.out.print("Пароль: ");
        String password = scanner.nextLine();

        User user = authService.login(login, password);
        System.out.println("Добро пожаловать, " + user.getLogin() + "!");
        System.out.println("Данные кошелька загружены. Баланс: " + user.getWallet().getBalance());
    }

    private static void handleWhoAmI(AuthService authService) {
        User user = authService.requireCurrentUser();
        System.out.println("Текущий пользователь: " + user.getLogin());
    }

    private static void handleLogout(AuthService authService) {
        authService.requireCurrentUser();
        authService.logout();
        System.out.println("Данные сохранены. Вы вышли из аккаунта.");
    }

    private static void handleIncome(Scanner scanner, FinanceService financeService) {
        System.out.print("Категория: ");
        String category = scanner.nextLine();
        System.out.print("Сумма: ");
        double amount = InputValidator.parsePositiveAmount(scanner.nextLine());

        Transaction transaction = financeService.addIncome(category, amount, null);
        System.out.println("Доход добавлен: " + transaction.getCategory().getName() + " — " + transaction.getAmount());
        System.out.println("Баланс: " + financeService.getBalance());
    }

    private static void handleExpense(Scanner scanner, FinanceService financeService) {
        System.out.print("Категория: ");
        String category = scanner.nextLine();
        System.out.print("Сумма: ");
        double amount = InputValidator.parsePositiveAmount(scanner.nextLine());

        List<String> alerts = financeService.addExpense(category, amount, null);
        System.out.println("Расход добавлен: " + category.trim() + " — " + amount);
        System.out.println("Баланс: " + financeService.getBalance());
        printAlerts(alerts);
    }

    private static void handleBudget(Scanner scanner, FinanceService financeService) {
        System.out.print("Категория: ");
        String category = scanner.nextLine();
        System.out.print("Лимит: ");
        double limit = InputValidator.parsePositiveAmount(scanner.nextLine());

        Budget budget = financeService.setBudget(category, limit);
        System.out.println("Бюджет установлен: " + budget.getCategory().getName() + " — " + budget.getLimit());

        Double remaining = financeService.getWallet().getRemainingBudget(budget.getCategory());
        if (remaining != null) {
            System.out.println("Оставшийся бюджет: " + remaining);
            if (remaining < 0) {
                printAlerts(List.of(
                        "Превышен бюджет по категории \"" + budget.getCategory().getName()
                                + "\": остаток " + remaining + "."
                ));
            }
        }
    }

    private static void handleBalance(FinanceService financeService) {
        Wallet wallet = financeService.getWallet();
        System.out.println("Баланс: " + wallet.getBalance());
        System.out.println("Доходы: " + wallet.getTotalIncome());
        System.out.println("Расходы: " + wallet.getTotalExpense());
        printAlerts(financeService.checkBudgetAlerts());
    }

    private static void handleBudgets(FinanceService financeService) {
        Wallet wallet = financeService.getWallet();
        Map<Category, Budget> budgets = wallet.getBudgets();
        if (budgets.isEmpty()) {
            System.out.println("Бюджеты не заданы.");
            return;
        }

        System.out.println("Бюджет по категориям:");
        for (Budget budget : budgets.values()) {
            double remaining = wallet.getRemainingBudget(budget.getCategory());
            System.out.println(budget.getCategory().getName() + ": " + budget.getLimit()
                    + ", Оставшийся бюджет: " + remaining);
        }
        printAlerts(financeService.checkBudgetAlerts());
    }

    private static void handleTransfer(
            Scanner scanner,
            FinanceService financeService,
            TransferService transferService
    ) {
        System.out.print("Логин получателя: ");
        String recipient = scanner.nextLine();
        System.out.print("Сумма: ");
        double amount = InputValidator.parsePositiveAmount(scanner.nextLine());

        List<String> alerts = transferService.transfer(recipient, amount);
        System.out.println("Перевод " + amount + " пользователю \"" + recipient.trim() + "\" выполнен.");
        System.out.println("Ваш баланс: " + financeService.getBalance());
        printAlerts(alerts);
    }

    private static void handleStats(StatisticsService statisticsService, ReportWriter reportWriter) {
        reportWriter.writeToConsole(statisticsService.buildFullReport());
    }

    private static void handleCategories(
            Scanner scanner,
            StatisticsService statisticsService,
            ReportWriter reportWriter
    ) {
        System.out.print("Категории через запятую: ");
        List<String> names = parseCategoryList(scanner.nextLine());
        reportWriter.writeToConsole(statisticsService.buildCategoryReport(names));
    }

    private static void handleReport(
            Scanner scanner,
            AuthService authService,
            StatisticsService statisticsService,
            ReportWriter reportWriter
    ) {
        User user = authService.requireCurrentUser();
        String report = statisticsService.buildFullReport();

        System.out.print("Куда вывести (console/file): ");
        String target = scanner.nextLine().trim().toLowerCase();

        if (target.isEmpty() || "console".equals(target) || "terminal".equals(target)) {
            reportWriter.writeToConsole(report);
            return;
        }

        if ("file".equals(target)) {
            System.out.print("Путь к файлу [data/" + user.getLogin() + "-report.txt]: ");
            String pathInput = scanner.nextLine().trim();
            Path path = pathInput.isEmpty()
                    ? Path.of("data", user.getLogin() + "-report.txt")
                    : Path.of(pathInput);
            Path saved = reportWriter.writeToFile(report, path);
            System.out.println("Отчёт сохранён: " + saved);
            return;
        }

        throw new IllegalArgumentException("Укажите console или file.");
    }

    private static List<String> parseCategoryList(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Список категорий пуст.");
        }
        List<String> names = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Список категорий пуст.");
        }
        return names;
    }

    private static void printHelp() {
        System.out.println("""
                Доступные команды:
                  register    — регистрация пользователя
                  login       — вход и загрузка кошелька из файла
                  whoami      — текущий пользователь
                  logout      — выход с сохранением кошелька
                  income      — добавить доход
                  expense     — добавить расход
                  budget      — установить бюджет категории
                  balance     — баланс, доходы и расходы
                  budgets     — бюджеты и оставшиеся лимиты
                  stats       — полный отчёт в терминал
                  categories  — подсчёт по выбранным категориям
                  report      — отчёт в терминал или файл
                  transfer    — перевод другому пользователю
                  help        — эта справка
                  exit        — сохранить данные и выйти
                """);
    }

    private static void printAlerts(List<String> alerts) {
        for (String alert : alerts) {
            System.out.println("Внимание: " + alert);
        }
    }
}
