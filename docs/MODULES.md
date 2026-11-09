# Документация по модулям

## `ru.fintrack` — точка входа

| Класс | Назначение |
|-------|------------|
| `Main` | Тонкая точка входа: делегирует запуск в `ConsoleApplication` |

## `ru.fintrack.model` — доменная модель

| Класс | Назначение |
|-------|------------|
| `User` | Пользователь: логин, пароль, привязанный `Wallet` |
| `Wallet` | Операции, бюджеты, подсчёт доходов/расходов и остатка бюджета |
| `Transaction` | Одна операция (сумма, категория, тип, комментарий) |
| `TransactionType` | Enum: `INCOME`, `EXPENSE` |
| `Category` | Категория (сравнение без учёта регистра) |
| `Budget` | Лимит бюджета по категории расходов |

## `ru.fintrack.repository` — хранилище пользователей

| Класс | Назначение |
|-------|------------|
| `UserRepository` | Интерфейс: сохранение и поиск пользователей в памяти |
| `InMemoryUserRepository` | Реализация на `Map` |
| `UserStorage` | Интерфейс персистентности пользователя/кошелька |

## `ru.fintrack.service` — бизнес-логика

| Класс | Назначение |
|-------|------------|
| `AuthService` | Регистрация, вход/выход, загрузка и сохранение кошелька |
| `FinanceService` | Доходы, расходы, CRUD бюджетов, переименование категорий |
| `AlertService` | Оповещения по бюджету/балансу (SRP) |
| `StatisticsService` | Полный отчёт и подсчёт по выбранным категориям |
| `CategoryStatsResult` | DTO результата подсчёта по категориям |
| `TransferService` | Перевод между пользователями по логину |

## `ru.fintrack.validation` — валидация

| Класс | Назначение |
|-------|------------|
| `InputValidator` | Проверка пустых строк и корректности сумм |

## `ru.fintrack.io` — ввод/вывод

| Класс | Назначение |
|-------|------------|
| `WalletFileStorage` | Сохранение/загрузка пользователя и кошелька в `data/*.wallet` |
| `ReportWriter` | Вывод текстового отчёта в консоль или в файл |
| `DataExchangeService` | Экспорт/импорт снимка кошелька в CSV и JSON |

## `ru.fintrack.ui` — представление CLI

| Класс | Назначение |
|-------|------------|
| `ConsoleApplication` | Цикл команд и композиция зависимостей |
| `Command` / `CommandRegistry` | Паттерн Command |
| `FinTrackCommands` | Регистрация обработчиков команд |
| `ConsoleSession` / `ConsoleInput` | Контекст и ввод |
| `ConsoleFormatter` | Таблицы баланса/бюджетов |

## Поток данных

```text
Консоль (Main)
    → AuthService / FinanceService / StatisticsService / TransferService
        → UserRepository (память)
        → WalletFileStorage (диск)
        → model.Wallet / Transaction / Budget
```
