# FinTrack

Консольное backend-приложение для управления личными финансами на **Java 17** (курс ООП).  
Пользователи регистрируются, учитывают доходы и расходы, задают и редактируют бюджеты, смотрят статистику, экспортируют/импортируют данные (CSV/JSON), сохраняют кошелёк в файл и переводят деньги между пользователями.

Внешние runtime-библиотеки **не используются**. Для тестов подключается JUnit 5 (только `test`-scope).

## Требования

- JDK 17+
- Apache Maven 3.9+

```bash
java -version
mvn -version
```

## Быстрый старт

```bash
cd fin-track
mvn compile exec:java
```

Сборка JAR:

```bash
mvn package
java -jar target/fin-track-1.0-SNAPSHOT.jar
```

Данные пользователей: `data/<login>.wallet`.

## Архитектура

```text
Main
  → ui.ConsoleApplication (цикл + DI)
      → CommandRegistry / FinTrackCommands (Command)
      → service (Auth / Finance / Alert / Statistics / Transfer)
          → repository (UserRepository, UserStorage)
          → io (WalletFileStorage, DataExchangeService, ReportWriter)
          → model (User, Wallet, Transaction, Budget, Category)
```

Слои:

| Пакет | Роль |
|-------|------|
| `model` | Доменные сущности, фабрики `Transaction.income/expense` |
| `repository` | Абстракции памяти (`UserRepository`) и диска (`UserStorage`) |
| `service` | Бизнес-логика; оповещения вынесены в `AlertService` |
| `validation` | Проверка ввода |
| `io` | Файлы кошелька, CSV/JSON, текстовые отчёты |
| `ui` | CLI: Command-паттерн, ввод, таблицы |

Подробнее: [docs/MODULES.md](docs/MODULES.md).

## Команды

| Команда | Описание |
|---------|----------|
| `register` / `login` / `logout` / `whoami` | Авторизация |
| `income` / `expense` | Доходы и расходы |
| `budget` / `edit-budget` / `remove-budget` | Бюджеты |
| `rename-category` | Переименование категории во всех операциях |
| `balance` / `budgets` | Таблицы баланса и бюджетов |
| `stats` / `categories` / `report` | Статистика и отчёты |
| `export` / `import` | Снимок кошелька в CSV/JSON и обратно |
| `transfer` | Перевод другому пользователю по логину |
| `help` / `exit` | Справка и выход с сохранением |

Примеры:

```text
> income
Категория: Зарплата
Сумма: 20000

> categories
Категории через запятую: Еда, Такси, Несуществующая

> export
Формат (csv/json): json
```

## Оповещения

- превышение бюджета категории;
- использование ≥ 80% лимита;
- расходы больше доходов;
- нулевой или отрицательный баланс.

## Как протестировать

### Автотесты

```bash
mvn test
```

Ожидается ≥ 15 unit/integration-тестов (валидация, кошелёк, auth, finance, stats, transfer, CSV/JSON).

### Проверка стиля и CI-локально

```bash
mvn verify
```

CI: `.github/workflows/ci.yml` (compile + test + checkstyle).  
Правила стиля: [`config/checkstyle.xml`](config/checkstyle.xml) (не Google Style — под 4 пробела и стиль проекта).

### Ручной сценарий из ТЗ

```bash
mvn -q exec:java
```

```text
register → alice/secret
login → alice/secret
income: Зарплата 20000, Зарплата 40000, Бонус 3000
expense: Еда 300, Еда 500, Развлечения 3000, Коммунальные услуги 3000, Такси 1500
budget: Еда 4000, Развлечения 3000, Коммунальные услуги 2500
stats
```

Ожидаемо: доход `63000.0`, расходы `8300.0`, остатки бюджетов `3200 / 0 / -500`.

### Персистентность и перевод

1. После операций — `exit`, снова `login` → баланс сохранён.
2. `transfer` другому зарегистрированному пользователю → расход у отправителя, доход «Перевод» у получателя.

## Соответствие критериям (цель — максимум)

- Базовый функционал ТЗ: auth, CLI-цикл, операции, кошелёк, бюджеты, статистика, оповещения, файлы, валидация, слои классов.
- Средний уровень: выбор категорий, редактирование бюджетов/категорий, CSV/JSON export+import, расширенные алерты, help + таблицы.
- Высокий уровень: JUnit-тесты, слоистая архитектура, Maven + Checkstyle + GitHub Actions, README с архитектурой и запуском тестов.
