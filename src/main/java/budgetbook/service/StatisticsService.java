package budgetbook.service;

import budgetbook.model.BalancePoint;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.CategoryTotal;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.PeriodSummary;
import budgetbook.model.PeriodTotals;
import budgetbook.model.Transaction;
import budgetbook.repository.Storage;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** 各種統計：期間收支、分類占比、每日／每月收支、資產趨勢。 */
public final class StatisticsService {
    private final Storage storage;
    private final WalletService wallets;
    private final Clock clock;

    public StatisticsService(Storage storage, WalletService wallets, Clock clock) {
        this.storage = storage;
        this.wallets = wallets;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public YearMonth currentMonth() {
        return YearMonth.from(today());
    }

    public PeriodSummary summary(DateRange range) {
        return summarize(range, storage.transactions().findBetween(range));
    }

    public static PeriodSummary summarize(DateRange range, List<Transaction> transactions) {
        Money income = Money.ZERO;
        Money expense = Money.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.isIncome()) {
                income = income.plus(transaction.amount());
            } else if (transaction.isExpense()) {
                expense = expense.plus(transaction.amount());
            }
        }
        return new PeriodSummary(range, income, expense, transactions.size());
    }

    /** 某段期間依分類加總，金額由大到小。 */
    public List<CategoryTotal> totalsByCategory(DateRange range, CategoryKind kind) {
        return totalsByCategory(storage.transactions().findBetween(range), kind, categoriesById());
    }

    public static List<CategoryTotal> totalsByCategory(List<Transaction> transactions, CategoryKind kind,
                                                       Map<Long, Category> categories) {
        Map<Long, Money> totals = new LinkedHashMap<>();
        Map<Long, Integer> counts = new HashMap<>();
        Money grand = Money.ZERO;
        for (Transaction t : transactions) {
            if (t.type() == kind.toTransactionType()) {
                totals.merge(t.categoryId(), t.amount(), Money::plus);
                counts.merge(t.categoryId(), 1, Integer::sum);
                grand = grand.plus(t.amount());
            }
        }
        List<CategoryTotal> result = new ArrayList<>();
        for (Map.Entry<Long, Money> entry : totals.entrySet()) {
            Category category = categories.get(entry.getKey());
            if (category != null) {
                result.add(new CategoryTotal(category, entry.getValue(), counts.get(entry.getKey()),
                        entry.getValue().ratioOf(grand)));
            }
        }
        result.sort(Comparator.comparing(CategoryTotal::total).reversed());
        return result;
    }

    /** 每個月的收入與支出，圖表的一根長條就是一個月。 */
    public List<PeriodTotals> monthlyTotals(YearMonth from, YearMonth to) {
        DateRange range = new DateRange(from.atDay(1), to.atEndOfMonth());
        List<Transaction> transactions = storage.transactions().findBetween(range);
        boolean multiYear = from.getYear() != to.getYear();
        List<PeriodTotals> result = new ArrayList<>();
        for (YearMonth month : range.months()) {
            String label = multiYear
                    ? String.format("%02d/%d", month.getYear() % 100, month.getMonthValue())
                    : month.getMonthValue() + "月";
            result.add(totalsFor(label, month.atDay(1),
                    transactions.stream().filter(t -> YearMonth.from(t.date()).equals(month)).toList()));
        }
        return result;
    }

    /** 每天的收入與支出。 */
    public List<PeriodTotals> dailyTotals(DateRange range) {
        Map<LocalDate, List<Transaction>> byDate = new HashMap<>();
        storage.transactions().findBetween(range)
                .forEach(t -> byDate.computeIfAbsent(t.date(), d -> new ArrayList<>()).add(t));
        List<PeriodTotals> result = new ArrayList<>();
        for (LocalDate date : range.dates()) {
            result.add(totalsFor(date.getMonthValue() + "/" + date.getDayOfMonth(), date,
                    byDate.getOrDefault(date, List.of())));
        }
        return result;
    }

    private static PeriodTotals totalsFor(String label, LocalDate start, List<Transaction> transactions) {
        PeriodSummary summary = summarize(DateRange.ofDay(start), transactions);
        return new PeriodTotals(label, start, summary.income(), summary.expense());
    }

    /** 每天結束時所有錢包的總額，用於資產趨勢折線圖。 */
    public List<BalancePoint> balanceTrend(DateRange range) {
        Money running = wallets.totalBalanceAsOf(range.start().minusDays(1));
        Map<LocalDate, Money> changes = new HashMap<>();
        storage.transactions().findBetween(range)
                .forEach(t -> changes.merge(t.date(), t.netWorthEffect(), Money::plus));
        List<BalancePoint> points = new ArrayList<>();
        for (LocalDate date : range.dates()) {
            running = running.plus(changes.getOrDefault(date, Money.ZERO));
            points.add(new BalancePoint(date, running));
        }
        return points;
    }

    /** 金額最大的一筆支出。 */
    public Optional<Transaction> largestExpense(DateRange range) {
        return storage.transactions().findBetween(range).stream()
                .filter(Transaction::isExpense)
                .max(Comparator.comparing(Transaction::amount));
    }

    /** 平均每日支出；期間包含今天時只算到今天。 */
    public Money averageDailyExpense(DateRange range) {
        LocalDate end = range.contains(today()) ? today() : range.end();
        long days = Math.max(1, ChronoUnit.DAYS.between(range.start(), end) + 1);
        return summary(range).expense().dividedBy(days);
    }

    public Map<Long, Category> categoriesById() {
        Map<Long, Category> map = new HashMap<>();
        storage.categories().findAll().forEach(c -> map.put(c.id(), c));
        return map;
    }

    public static <T> Map<Long, T> index(List<T> items, Function<T, Long> id) {
        Map<Long, T> map = new LinkedHashMap<>();
        items.forEach(item -> map.put(id.apply(item), item));
        return map;
    }
}
