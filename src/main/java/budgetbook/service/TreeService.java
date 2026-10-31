package budgetbook.service;

import budgetbook.model.DateRange;
import budgetbook.model.Forest;
import budgetbook.model.MonthlyTree;
import budgetbook.model.Transaction;
import budgetbook.repository.Storage;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 種樹遊戲化。每記一筆帳就是替本月小樹澆一次水；定期交易自動入帳的紀錄不算，
 * 因為那不是使用者自己記的。
 */
public final class TreeService {
    private final Storage storage;
    private final SettingsService settings;
    private final Clock clock;

    public TreeService(Storage storage, SettingsService settings, Clock clock) {
        this.storage = storage;
        this.settings = settings;
        this.clock = clock;
    }

    public int goal() {
        return settings.treeGoal();
    }

    public MonthlyTree treeFor(YearMonth month) {
        long records = storage.transactions().findBetween(DateRange.ofMonth(month)).stream()
                .filter(TreeService::isManual).count();
        return new MonthlyTree(month, (int) records, goal());
    }

    public MonthlyTree currentTree() {
        return treeFor(YearMonth.now(clock));
    }

    /** 從第一筆記帳的月份到本月（最多 {@code maxMonths} 個月），每月一棵樹，外加連續記帳天數。 */
    public Forest forest(int maxMonths) {
        LocalDate today = LocalDate.now(clock);
        YearMonth current = YearMonth.from(today);
        YearMonth first = storage.transactions().earliestDate().map(YearMonth::from).orElse(current);
        if (first.isAfter(current)) {
            first = current;
        }
        List<Transaction> manual = storage.transactions()
                .findBetween(new DateRange(first.atDay(1), current.atEndOfMonth())).stream()
                .filter(TreeService::isManual).toList();

        Map<YearMonth, Integer> counts = new HashMap<>();
        NavigableSet<LocalDate> days = new TreeSet<>();
        for (Transaction transaction : manual) {
            counts.merge(transaction.yearMonth(), 1, Integer::sum);
            if (!transaction.date().isAfter(today)) {
                days.add(transaction.date());
            }
        }
        YearMonth start = first.isBefore(current.minusMonths(maxMonths - 1L)) ? current.minusMonths(maxMonths - 1L) : first;
        List<MonthlyTree> trees = new ArrayList<>();
        int goal = goal();
        for (YearMonth month = start; !month.isAfter(current); month = month.plusMonths(1)) {
            trees.add(new MonthlyTree(month, counts.getOrDefault(month, 0), goal));
        }
        return new Forest(trees, currentStreak(days, today), longestStreak(days));
    }

    private static boolean isManual(Transaction transaction) {
        return transaction.recurringRuleId() == null;
    }

    /** 到今天為止連續有記帳的天數；今天還沒記但昨天有記，連續紀錄仍然保留。 */
    static int currentStreak(Set<LocalDate> days, LocalDate today) {
        LocalDate day = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    static int longestStreak(NavigableSet<LocalDate> days) {
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate day : days) {
            run = previous != null && previous.plusDays(1).equals(day) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = day;
        }
        return longest;
    }
}
