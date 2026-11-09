package ru.fintrack.ui;

import java.nio.file.Path;
import java.util.List;

import ru.fintrack.model.Budget;
import ru.fintrack.model.Transaction;
import ru.fintrack.model.User;
import ru.fintrack.model.Wallet;

/**
 * Фабрика регистрации команд приложения (изоляция CLI от Main).
 */
public final class FinTrackCommands {

    private FinTrackCommands() {
    }

    public static CommandRegistry create() {
        CommandRegistry registry = new CommandRegistry();

        registry.register("register", FinTrackCommands::register);
        registry.register("login", FinTrackCommands::login);
        registry.register("whoami", FinTrackCommands::whoAmI);
        registry.register("logout", FinTrackCommands::logout);
        registry.register("income", FinTrackCommands::income);
        registry.register("expense", FinTrackCommands::expense);
        registry.register("budget", FinTrackCommands::budget);
        registry.register("edit-budget", FinTrackCommands::editBudget);
        registry.register("remove-budget", FinTrackCommands::removeBudget);
        registry.register("rename-category", FinTrackCommands::renameCategory);
        registry.register("balance", FinTrackCommands::balance);
        registry.register("budgets", FinTrackCommands::budgets);
        registry.register("stats", FinTrackCommands::stats);
        registry.register("categories", FinTrackCommands::categories);
        registry.register("report", FinTrackCommands::report);
        registry.register("export", FinTrackCommands::exportData);
        registry.register("import", FinTrackCommands::importData);
        registry.register("transfer", FinTrackCommands::transfer);
        registry.register("help", FinTrackCommands::help);
        registry.registerAliases(FinTrackCommands::exit, "exit", "quit");

        return registry;
    }

    private static boolean register(ConsoleSession session) {
        ConsoleInput in = session.input();
        String login = in.readLine("Логин: ");
        String password = in.readLine("Пароль: ");
        User user = session.auth().register(login, password);
        System.out.println("Пользователь \"" + user.getLogin() + "\" зарегистрирован.");
        System.out.println("Файл кошелька: data/" + user.getLogin().toLowerCase() + ".wallet");
        return true;
    }

    private static boolean login(ConsoleSession session) {
        ConsoleInput in = session.input();
        User user = session.auth().login(in.readLine("Логин: "), in.readLine("Пароль: "));
        System.out.println("Добро пожаловать, " + user.getLogin() + "!");
        System.out.println("Данные кошелька загружены. Баланс: " + user.getWallet().getBalance());
        return true;
    }

    private static boolean whoAmI(ConsoleSession session) {
        System.out.println("Текущий пользователь: " + session.auth().requireCurrentUser().getLogin());
        return true;
    }

    private static boolean logout(ConsoleSession session) {
        session.auth().requireCurrentUser();
        session.auth().logout();
        System.out.println("Данные сохранены. Вы вышли из аккаунта.");
        return true;
    }

    private static boolean income(ConsoleSession session) {
        ConsoleInput in = session.input();
        String category = in.readLine("Категория: ");
        double amount = in.readPositiveAmount("Сумма: ");
        Transaction tx = session.finance().addIncome(category, amount, null);
        System.out.println("Доход добавлен: " + tx.getCategory().getName() + " — " + tx.getAmount());
        System.out.println("Баланс: " + session.finance().getBalance());
        return true;
    }

    private static boolean expense(ConsoleSession session) {
        ConsoleInput in = session.input();
        String category = in.readLine("Категория: ");
        double amount = in.readPositiveAmount("Сумма: ");
        List<String> alerts = session.finance().addExpense(category, amount, null);
        System.out.println("Расход добавлен: " + category.trim() + " — " + amount);
        System.out.println("Баланс: " + session.finance().getBalance());
        ConsoleFormatter.printAlerts(alerts);
        return true;
    }

    private static boolean budget(ConsoleSession session) {
        ConsoleInput in = session.input();
        Budget budget = session.finance().setBudget(in.readLine("Категория: "), in.readPositiveAmount("Лимит: "));
        System.out.println("Бюджет установлен: " + budget.getCategory().getName() + " — " + budget.getLimit());
        Double remaining = session.finance().getWallet().getRemainingBudget(budget.getCategory());
        if (remaining != null) {
            System.out.println("Оставшийся бюджет: " + remaining);
        }
        ConsoleFormatter.printAlerts(session.finance().checkBudgetAlerts());
        return true;
    }

    private static boolean editBudget(ConsoleSession session) {
        ConsoleInput in = session.input();
        Budget budget = session.finance().updateBudget(
                in.readLine("Категория: "),
                in.readPositiveAmount("Новый лимит: ")
        );
        System.out.println("Бюджет обновлён: " + budget.getCategory().getName() + " — " + budget.getLimit());
        ConsoleFormatter.printAlerts(session.finance().checkBudgetAlerts());
        return true;
    }

    private static boolean removeBudget(ConsoleSession session) {
        String category = session.input().readLine("Категория: ");
        session.finance().removeBudget(category);
        System.out.println("Бюджет для категории \"" + category.trim() + "\" удалён.");
        return true;
    }

    private static boolean renameCategory(ConsoleSession session) {
        ConsoleInput in = session.input();
        int changed = session.finance().renameCategory(in.readLine("Старое имя: "), in.readLine("Новое имя: "));
        System.out.println("Категория переименована. Затронуто операций: " + changed + ".");
        return true;
    }

    private static boolean balance(ConsoleSession session) {
        System.out.println(ConsoleFormatter.formatBalanceSummary(session.finance().getWallet()));
        ConsoleFormatter.printAlerts(session.finance().checkBudgetAlerts());
        return true;
    }

    private static boolean budgets(ConsoleSession session) {
        System.out.println(ConsoleFormatter.formatBudgetTable(session.finance().getWallet()));
        ConsoleFormatter.printAlerts(session.finance().checkBudgetAlerts());
        return true;
    }

    private static boolean stats(ConsoleSession session) {
        session.reports().writeToConsole(session.statistics().buildFullReport());
        return true;
    }

    private static boolean categories(ConsoleSession session) {
        List<String> names = session.input().readCategoryList("Категории через запятую: ");
        session.reports().writeToConsole(session.statistics().buildCategoryReport(names));
        return true;
    }

    private static boolean report(ConsoleSession session) {
        User user = session.auth().requireCurrentUser();
        String report = session.statistics().buildFullReport();
        String target = session.input().readLine("Куда вывести (console/file): ").trim().toLowerCase();

        if (target.isEmpty() || "console".equals(target) || "terminal".equals(target)) {
            session.reports().writeToConsole(report);
            return true;
        }
        if ("file".equals(target)) {
            String pathInput = session.input()
                    .readLine("Путь к файлу [data/" + user.getLogin() + "-report.txt]: ")
                    .trim();
            Path path = pathInput.isEmpty()
                    ? Path.of("data", user.getLogin() + "-report.txt")
                    : Path.of(pathInput);
            Path saved = session.reports().writeToFile(report, path);
            System.out.println("Отчёт сохранён: " + saved);
            return true;
        }
        throw new IllegalArgumentException("Укажите console или file.");
    }

    private static boolean exportData(ConsoleSession session) {
        User user = session.auth().requireCurrentUser();
        String format = session.input().readLine("Формат (csv/json): ").trim().toLowerCase();
        Wallet wallet = session.finance().getWallet();
        if ("csv".equals(format)) {
            Path saved = session.exchange().exportCsv(wallet, Path.of("data", user.getLogin() + "-export.csv"));
            System.out.println("CSV экспортирован: " + saved);
            return true;
        }
        if ("json".equals(format)) {
            Path saved = session.exchange().exportJson(wallet, Path.of("data", user.getLogin() + "-export.json"));
            System.out.println("JSON экспортирован: " + saved);
            return true;
        }
        throw new IllegalArgumentException("Поддерживаются форматы: csv, json.");
    }

    private static boolean importData(ConsoleSession session) {
        String format = session.input().readLine("Формат (csv/json): ").trim().toLowerCase();
        Path path = Path.of(session.input().readLine("Путь к файлу: ").trim());
        if ("csv".equals(format)) {
            session.exchange().importCsv(session.finance().getWallet(), path);
        } else if ("json".equals(format)) {
            session.exchange().importJson(session.finance().getWallet(), path);
        } else {
            throw new IllegalArgumentException("Поддерживаются форматы: csv, json.");
        }
        System.out.println("Данные импортированы. Баланс: " + session.finance().getBalance());
        ConsoleFormatter.printAlerts(session.finance().checkBudgetAlerts());
        return true;
    }

    private static boolean transfer(ConsoleSession session) {
        ConsoleInput in = session.input();
        String recipient = in.readLine("Логин получателя: ");
        double amount = in.readPositiveAmount("Сумма: ");
        List<String> alerts = session.transfers().transfer(recipient, amount);
        System.out.println("Перевод " + amount + " пользователю \"" + recipient.trim() + "\" выполнен.");
        System.out.println("Ваш баланс: " + session.finance().getBalance());
        ConsoleFormatter.printAlerts(alerts);
        return true;
    }

    private static boolean help(ConsoleSession session) {
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
        return true;
    }

    private static boolean exit(ConsoleSession session) {
        session.auth().saveCurrentUser();
        System.out.println("Данные сохранены. Выход из приложения.");
        return false;
    }
}
