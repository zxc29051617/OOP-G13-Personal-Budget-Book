package budgetbook.service.report;

import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.PeriodSummary;
import budgetbook.model.Transaction;
import budgetbook.model.Wallet;
import budgetbook.repository.Storage;
import budgetbook.service.BudgetService;
import budgetbook.service.StatisticsService;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 依 {@link ReportPeriod} 策略產生月報或年報。 */
public final class ReportService {
    private static final int TOP_EXPENSES = 10;

    private final Storage storage;
    private final StatisticsService statistics;
    private final BudgetService budgets;
    private final Clock clock;

    public ReportService(Storage storage, StatisticsService statistics, BudgetService budgets, Clock clock) {
        this.storage = storage;
        this.statistics = statistics;
        this.budgets = budgets;
        this.clock = clock;
    }

    public Report generate(ReportPeriod period) {
        List<Transaction> transactions = storage.transactions().findBetween(period.range());
        PeriodSummary summary = StatisticsService.summarize(period.range(), transactions);
        PeriodSummary previous = statistics.summary(period.previous().range());
        Map<Long, Category> categories = statistics.categoriesById();
        Map<Long, String> walletNames = new HashMap<>();
        storage.wallets().findAll().forEach(w -> walletNames.put(w.id(), w.name()));

        List<Report.Item> top = transactions.stream()
                .filter(Transaction::isExpense)
                .sorted(Comparator.comparing(Transaction::amount).reversed().thenComparing(Transaction::date))
                .limit(TOP_EXPENSES)
                .map(t -> new Report.Item(t.date(), categories.get(t.categoryId()).name(),
                        walletNames.getOrDefault(t.walletId(), ""), t.note(), t.amount()))
                .toList();

        return new Report(period, summary, previous,
                StatisticsService.totalsByCategory(transactions, CategoryKind.EXPENSE, categories),
                StatisticsService.totalsByCategory(transactions, CategoryKind.INCOME, categories),
                period.series(statistics),
                period instanceof MonthlyPeriod monthly ? budgets.statuses(monthly.month()) : List.of(),
                top,
                LocalDateTime.now(clock).withNano(0));
    }

    public String export(Report report, ReportExporter exporter) {
        StringWriter writer = new StringWriter();
        try {
            exporter.write(report, writer);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }

    /** 本月與今年，給報表頁預設使用。 */
    public ReportPeriod currentMonth() {
        return new MonthlyPeriod(YearMonth.now(clock));
    }

    /** 錢包名稱（給沒有 Wallet 物件的地方使用）。 */
    public String walletName(long id) {
        return storage.wallets().findById(id).map(Wallet::name).orElse("");
    }
}
