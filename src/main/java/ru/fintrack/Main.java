package ru.fintrack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import ru.fintrack.io.DataExchangeService;
import ru.fintrack.io.ReportWriter;
import ru.fintrack.io.WalletFileStorage;
import ru.fintrack.model.Budget;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.User;
import ru.fintrack.model.Wallet;
import ru.fintrack.repository.InMemoryUserRepository;
import ru.fintrack.repository.UserRepository;
import ru.fintrack.service.AuthService;
import ru.fintrack.service.FinanceService;
import ru.fintrack.service.StatisticsService;
import ru.fintrack.service.TransferService;
import ru.fintrack.ui.ConsoleFormatter;
import ru.fintrack.validation.InputValidator;

/**
 * Точка входа: цикл команд авторизации, финансов, статистики и переводов.
 */
public class Main {

    private static final String COMMANDS = String.join(
            " | ",
            "register", "login", "whoami", "logout",
            "income", "expense", "budget", "edit-budget", "remove-budget", "rename-category",
            "balance", "budgets", "stats", "categories", "report",
            "export", "import", "transfer", "help", "exit"
    );

    public static void main(String[] args) {
        WalletFileStorage walletFileStorage = new WalletFileStorage(Path.of("data"));
        UserRepository userRepository = new InMemoryUserRepository();
        AuthService authService = new AuthService(userRepository, walletFileStorage);
        FinanceService financeService = new FinanceService(authService);
        StatisticsService statisticsService = new StatisticsService(financeService);
        TransferService transferService = new TransferService(authService, userRepository, walletFileStorage);
        ReportWriter reportWriter = new ReportWriter();
        DataExchangeService dataExchangeService = new DataExchangeService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== FinTrack ===");
        System.out.println("Введите help для списка команд и примеров.");

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
                    case "edit-budget" -> handleEditBudget(scanner, financeService);
                    case "remove-budget" -> handleRemoveBudget(scanner, financeService);
                    case "rename-category" -> handleRenameCategory(scanner, financeService);
                    case "balance" -> handleBalance(financeService);
                    case "budgets" -> handleBudgets(financeService);
                    case "stats" -> handleStats(statisticsService, reportWriter);
                    case "categories" -> handleCategories(scanner, statisticsService, reportWriter);
                    case "report" -> handleReport(scanner, authService, statisticsService, reportWriter);
                    case "export" -> handleExport(scanner, authService, financeService, dataExchangeService);
                    case "import" -> handleImport(scanner, financeService, dataExchangeService);
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
        ConsoleFormatter.printAlerts(alerts);
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
        }
        ConsoleFormatter.printAlerts(financeService.checkBudgetAlerts());
    }

    private static void handleEditBudget(Scanner scanner, FinanceService financeService) {
        System.out.print("Категория: ");
        String category = scanner.nextLine();
        System.out.print("Новый лимит: ");
        double limit = InputValidator.parsePositiveAmount(scanner.nextLine());
        Budget budget = financeService.updateBudget(category, limit);
        System.out.println("Бюджет обновлён: " + budget.getCategory().getName() + " — " + budget.getLimit());
        ConsoleFormatter.printAlerts(financeService.checkBudgetAlerts());
    }

    private static void handleRemoveBudget(Scanner scanner, FinanceService financeService) {
        System.out.print("Категория: ");
        String category = scanner.nextLine();
        financeService.removeBudget(category);
        System.out.println("Бюджет для категории \"" + category.trim() + "\" удалён.");
    }

    private static void handleRenameCategory(Scanner scanner, FinanceService financeService) {
        System.out.print("Старое имя: ");
        String oldName = scanner.nextLine();
        System.out.print("Новое имя: ");
        String newName = scanner.nextLine();
        int changed = financeService.renameCategory(oldName, newName);
        System.out.println("Категория переименована. Затронуто операций: " + changed + ".");
    }

    private static void handleBalance(FinanceService financeService) {
        System.out.println(ConsoleFormatter.formatBalanceSummary(financeService.getWallet()));
        ConsoleFormatter.printAlerts(financeService.checkBudgetAlerts());
    }

    private static void handleBudgets(FinanceService financeService) {
        System.out.println(ConsoleFormatter.formatBudgetTable(financeService.getWallet()));
        ConsoleFormatter.printAlerts(financeService.checkBudgetAlerts());
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
        ConsoleFormatter.printAlerts(alerts);
        ConsoleFormatter.printAlerts(financeService.checkBudgetAlerts());
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

    private static void handleExport(
            Scanner scanner,
            AuthService authService,
            FinanceService financeService,
            DataExchangeService dataExchangeService
    ) {
        User user = authService.requireCurrentUser();
        System.out.print("Формат (csv/json): ");
        String format = scanner.nextLine().trim().toLowerCase();
        Wallet wallet = financeService.getWallet();

        if ("csv".equals(format)) {
            Path path = Path.of("data", user.getLogin() + "-export.csv");
            Path saved = dataExchangeService.exportCsv(wallet, path);
            System.out.println("CSV экспортирован: " + saved);
            return;
        }
        if ("json".equals(format)) {
            Path path = Path.of("data", user.getLogin() + "-export.json");
            Path saved = dataExchangeService.exportJson(wallet, path);
            System.out.println("JSON экспортирован: " + saved);
            return;
        }
        throw new IllegalArgumentException("Поддерживаются форматы: csv, json.");
    }

    private static void handleImport(
            Scanner scanner,
            FinanceService financeService,
            DataExchangeService dataExchangeService
    ) {
        financeService.getWallet();
        System.out.print("Формат (csv/json): ");
        String format = scanner.nextLine().trim().toLowerCase();
        System.out.print("Путь к файлу: ");
        Path path = Path.of(scanner.nextLine().trim());

        if ("csv".equals(format)) {
            dataExchangeService.importCsv(financeService.getWallet(), path);
        } else if ("json".equals(format)) {
            dataExchangeService.importJson(financeService.getWallet(), path);
        } else {
            throw new IllegalArgumentException("Поддерживаются форматы: csv, json.");
        }
        System.out.println("Данные импортированы. Баланс: " + financeService.getBalance());
        ConsoleFormatter.printAlerts(financeService.checkBudgetAlerts());
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
                  register         — регистрация (пример: alice / secret)
                  login            — вход и загрузка кошелька
                  whoami           — текущий пользователь
                  logout           — выход с сохранением
                  income           — доход: категория + сумма
                  expense          — расход: категория + сумма
                  budget           — создать бюджет категории
                  edit-budget      — изменить лимит бюджета
                  remove-budget    — удалить бюджет
                  rename-category  — переименовать категорию во всех операциях
                  balance          — таблица баланс/доходы/расходы
                  budgets          — таблица бюджетов и остатков
                  stats            — полный отчёт
                  categories       — подсчёт по списку категорий через запятую
                  report           — отчёт в console или file
                  export           — экспорт снимка в csv/json
                  import           — импорт снимка из csv/json
                  transfer         — перевод другому пользователю по логину
                  help             — эта справка с примерами
                  exit             — сохранить и выйти

                Примеры:
                  > income
                    Категория: Зарплата
                    Сумма: 20000
                  > categories
                    Категории через запятую: Еда, Такси
                  > export
                    Формат (csv/json): json
                """);
        System.out.println("Краткий список: " + COMMANDS);
    }
}
