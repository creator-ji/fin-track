package ru.fintrack.ui;

import ru.fintrack.io.DataExchangeService;
import ru.fintrack.io.ReportWriter;
import ru.fintrack.service.AuthService;
import ru.fintrack.service.FinanceService;
import ru.fintrack.service.StatisticsService;
import ru.fintrack.service.TransferService;

/**
 * Контекст выполнения CLI-команд (зависимости сессии).
 */
public class ConsoleSession {

    private final ConsoleInput input;
    private final AuthService authService;
    private final FinanceService financeService;
    private final StatisticsService statisticsService;
    private final TransferService transferService;
    private final ReportWriter reportWriter;
    private final DataExchangeService dataExchangeService;

    public ConsoleSession(
            ConsoleInput input,
            AuthService authService,
            FinanceService financeService,
            StatisticsService statisticsService,
            TransferService transferService,
            ReportWriter reportWriter,
            DataExchangeService dataExchangeService
    ) {
        this.input = input;
        this.authService = authService;
        this.financeService = financeService;
        this.statisticsService = statisticsService;
        this.transferService = transferService;
        this.reportWriter = reportWriter;
        this.dataExchangeService = dataExchangeService;
    }

    public ConsoleInput input() {
        return input;
    }

    public AuthService auth() {
        return authService;
    }

    public FinanceService finance() {
        return financeService;
    }

    public StatisticsService statistics() {
        return statisticsService;
    }

    public TransferService transfers() {
        return transferService;
    }

    public ReportWriter reports() {
        return reportWriter;
    }

    public DataExchangeService exchange() {
        return dataExchangeService;
    }
}
