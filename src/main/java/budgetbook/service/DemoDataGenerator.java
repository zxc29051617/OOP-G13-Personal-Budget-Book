package budgetbook.service;

import budgetbook.model.Budget;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Frequency;
import budgetbook.model.Money;
import budgetbook.model.RecurringRule;
import budgetbook.model.SavingsGoal;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.repository.Storage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 產生最近六個月的示範資料：上班族的日常收支、定期交易、預算與儲蓄目標。
 * 使用固定亂數種子，所以同一天產生的資料都一樣，方便截圖與測試。
 */
final class DemoDataGenerator {
    /** 每月的記帳頻率，第三個月故意偏低，森林裡會有一棵沒長大的樹。 */
    private static final double[] MONTH_ACTIVITY = {1.0, 0.9, 0.1, 0.85, 1.0, 1.0};

    private record Habit(String category, double probability, int min, int max, LocalTime from, LocalTime to,
                         String[] wallets, String... notes) {
    }

    private static final List<Habit> HABITS = List.of(
            new Habit("餐飲", 0.30, 45, 95, LocalTime.of(7, 30), LocalTime.of(9, 30),
                    new String[]{"行動支付", "現金"}, "早餐", "咖啡", "蛋餅豆漿", "超商早餐"),
            new Habit("餐飲", 0.62, 80, 170, LocalTime.of(11, 45), LocalTime.of(13, 15),
                    new String[]{"現金", "行動支付"}, "午餐", "便當", "滷肉飯", "牛肉麵", "自助餐", "拉麵"),
            new Habit("餐飲", 0.45, 100, 280, LocalTime.of(18, 0), LocalTime.of(20, 30),
                    new String[]{"現金", "信用卡", "行動支付"}, "晚餐", "火鍋", "義大利麵", "夜市", "咖哩飯"),
            new Habit("餐飲", 0.18, 45, 75, LocalTime.of(14, 0), LocalTime.of(17, 0),
                    new String[]{"行動支付"}, "手搖飲", "珍珠奶茶"),
            new Habit("交通", 0.35, 20, 60, LocalTime.of(8, 0), LocalTime.of(9, 0),
                    new String[]{"行動支付"}, "捷運", "公車", "YouBike"),
            new Habit("交通", 0.03, 180, 320, LocalTime.of(21, 0), LocalTime.of(23, 0),
                    new String[]{"信用卡"}, "計程車"),
            new Habit("生活", 0.10, 39, 260, LocalTime.of(19, 0), LocalTime.of(22, 0),
                    new String[]{"現金", "行動支付"}, "日用品", "超商", "洗衣"),
            new Habit("購物", 0.05, 290, 1800, LocalTime.of(20, 0), LocalTime.of(23, 0),
                    new String[]{"信用卡"}, "網購", "衣服", "鞋子", "生活雜貨"),
            new Habit("娛樂", 0.05, 250, 650, LocalTime.of(19, 0), LocalTime.of(22, 0),
                    new String[]{"信用卡", "現金"}, "電影", "KTV", "桌遊", "展覽"),
            new Habit("醫療", 0.012, 150, 650, LocalTime.of(10, 0), LocalTime.of(17, 0),
                    new String[]{"現金"}, "診所", "藥局"),
            new Habit("學習", 0.015, 280, 990, LocalTime.of(20, 0), LocalTime.of(23, 0),
                    new String[]{"信用卡"}, "書籍", "線上課程"));

    private final Storage storage;
    private final LocalDate today;
    private final Random random = new Random(13);
    private final Map<String, Long> wallets = new HashMap<>();
    private final Map<String, Long> expenseCategories = new HashMap<>();
    private final Map<String, Long> incomeCategories = new HashMap<>();

    DemoDataGenerator(Storage storage, LocalDate today) {
        this.storage = storage;
        this.today = today;
    }

    void generate() {
        DemoDataService.createDefaultCategories(storage);
        for (Category category : storage.categories().findAll()) {
            (category.kind() == CategoryKind.EXPENSE ? expenseCategories : incomeCategories)
                    .put(category.name(), category.id());
        }
        wallet("現金", WalletType.CASH, 2_000, 0x4C9A62);
        wallet("薪轉帳戶", WalletType.BANK, 32_000, 0x457B9D);
        wallet("行動支付", WalletType.E_PAYMENT, 800, 0xF4A261);
        wallet("信用卡", WalletType.CREDIT_CARD, 0, 0xD16BA5);
        wallet("儲蓄帳戶", WalletType.BANK, 85_000, 0x2A9D8F);

        YearMonth firstMonth = YearMonth.from(today).minusMonths(5);
        dailyLife(firstMonth);
        monthlyExtras(firstMonth);
        recurringRules(firstMonth);
        budgets();
        goals();
    }

    private void wallet(String name, WalletType type, long initial, int color) {
        wallets.put(name, storage.wallets().save(Wallet.create(name, type, Money.of(initial), color)).id());
    }

    private void dailyLife(YearMonth firstMonth) {
        for (LocalDate date = firstMonth.atDay(1); !date.isAfter(today); date = date.plusDays(1)) {
            int monthIndex = (int) ChronoUnit.MONTHS.between(firstMonth, YearMonth.from(date));
            double activity = MONTH_ACTIVITY[Math.min(monthIndex, MONTH_ACTIVITY.length - 1)];
            for (Habit habit : HABITS) {
                if (random.nextDouble() < habit.probability() * activity) {
                    expense(habit.category(), date, randomTime(habit.from(), habit.to()),
                            roundTo5(habit.min() + random.nextInt(habit.max() - habit.min() + 1)),
                            pick(habit.wallets()), pick(habit.notes()));
                }
            }
        }
    }

    private void monthlyExtras(YearMonth firstMonth) {
        for (int i = 0; i < 6; i++) {
            YearMonth month = firstMonth.plusMonths(i);
            transfer(month.atDay(3), "薪轉帳戶", "現金", 3_000, "提款");
            transfer(month.atDay(10), "薪轉帳戶", "行動支付", 1_500, "儲值");
            transfer(month.atDay(24), "薪轉帳戶", "信用卡", 3_000 + 250L * i, "繳信用卡費");
        }
        income("獎金", firstMonth.plusMonths(1).atDay(28), 6_000, "績效獎金");
        income("打工", firstMonth.plusMonths(3).atDay(9), 1_600, "家教");
        income("打工", firstMonth.plusMonths(3).atDay(23), 1_600, "家教");
        income("投資", firstMonth.plusMonths(4).atDay(15), 1_280, "股利");
        income("其他", firstMonth.plusMonths(5).atDay(2), 650, "二手拍賣");
        // 本月的大筆花費：讓預算頁出現「接近上限」與「已超支」
        expense("娛樂", firstMonth.plusMonths(5).atDay(4), LocalTime.of(20, 15), 2_800, "信用卡", "演唱會門票");
        expense("購物", firstMonth.plusMonths(5).atDay(7), LocalTime.of(21, 40), 1_990, "信用卡", "藍牙耳機");
    }

    private void recurringRules(YearMonth firstMonth) {
        rule("薪資", TransactionType.INCOME, 42_000, "薪資", "薪轉帳戶", null, firstMonth.atDay(5), Frequency.MONTHLY);
        rule("房租", TransactionType.EXPENSE, 9_500, "居住", "薪轉帳戶", null, firstMonth.atDay(1), Frequency.MONTHLY);
        rule("手機月租", TransactionType.EXPENSE, 499, "生活", "信用卡", null, firstMonth.atDay(12), Frequency.MONTHLY);
        rule("音樂串流", TransactionType.EXPENSE, 149, "娛樂", "信用卡", null, firstMonth.atDay(18), Frequency.MONTHLY);
        rule("每月存款", TransactionType.TRANSFER, 5_000, null, "薪轉帳戶", "儲蓄帳戶", firstMonth.atDay(6),
                Frequency.MONTHLY);
        rule("年繳保險", TransactionType.EXPENSE, 12_800, "生活", "薪轉帳戶", null, today.plusDays(20),
                Frequency.YEARLY);
    }

    private void rule(String name, TransactionType type, long amount, String category, String wallet,
                      String toWallet, LocalDate start, Frequency frequency) {
        Long categoryId = category == null ? null
                : (type == TransactionType.INCOME ? incomeCategories : expenseCategories).get(category);
        RecurringRule rule = storage.recurringRules().save(new RecurringRule(0, name, type, Money.of(amount),
                categoryId, wallets.get(wallet), toWallet == null ? null : wallets.get(toWallet), "", frequency, 1,
                start, null, 0, true));
        List<LocalDate> due = rule.dueDates(today);
        due.forEach(date -> storage.transactions().save(rule.toTransaction(date)));
        storage.recurringRules().save(rule.withPostedCount(due.size()));
    }

    private void budgets() {
        storage.budgets().save(Budget.overall(Money.of(26_000)));
        storage.budgets().save(Budget.forCategory(expenseCategories.get("餐飲"), Money.of(7_000)));
        storage.budgets().save(Budget.forCategory(expenseCategories.get("交通"), Money.of(1_800)));
        storage.budgets().save(Budget.forCategory(expenseCategories.get("購物"), Money.of(3_500)));
        storage.budgets().save(Budget.forCategory(expenseCategories.get("娛樂"), Money.of(2_500)));
    }

    private void goals() {
        YearMonth month = YearMonth.from(today);
        storage.savingsGoals().save(new SavingsGoal(0, "日本旅行", Money.of(60_000), Money.of(23_500),
                month.plusMonths(5).atEndOfMonth(), 0xE76F51));
        storage.savingsGoals().save(new SavingsGoal(0, "緊急預備金", Money.of(100_000), Money.of(64_000), null,
                0x2A9D8F));
        storage.savingsGoals().save(new SavingsGoal(0, "新筆電", Money.of(36_000), Money.of(12_400),
                month.plusMonths(3).atEndOfMonth(), 0x5E60CE));
    }

    private void expense(String category, LocalDate date, LocalTime time, long amount, String wallet, String note) {
        if (date.isAfter(today)) {
            return;
        }
        storage.transactions().save(Transaction.builder().expense().date(date).time(time).amount(amount)
                .categoryId(expenseCategories.get(category)).walletId(wallets.get(wallet)).note(note).build());
    }

    private void income(String category, LocalDate date, long amount, String note) {
        if (date.isAfter(today)) {
            return;
        }
        storage.transactions().save(Transaction.builder().income().date(date).time(LocalTime.of(10, 0))
                .amount(amount).categoryId(incomeCategories.get(category)).walletId(wallets.get("薪轉帳戶"))
                .note(note).build());
    }

    private void transfer(LocalDate date, String from, String to, long amount, String note) {
        if (date.isAfter(today)) {
            return;
        }
        storage.transactions().save(Transaction.builder().transfer(wallets.get(from), wallets.get(to))
                .date(date).time(LocalTime.of(12, 30)).amount(amount).note(note).build());
    }

    private LocalTime randomTime(LocalTime from, LocalTime to) {
        int span = Math.max(1, (to.toSecondOfDay() - from.toSecondOfDay()) / 60);
        return from.plusMinutes(random.nextInt(span));
    }

    private static long roundTo5(int value) {
        return Math.max(5, Math.round(value / 5.0) * 5);
    }

    private String pick(String[] options) {
        return options[random.nextInt(options.length)];
    }
}
