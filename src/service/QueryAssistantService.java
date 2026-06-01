package service;

import model.Account;
import model.Transaction;
import persistence.BudgetBookStore;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class QueryAssistantService {
    private static final DateTimeFormatter RECENT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BudgetBookStore store;
    private final StatsService stats;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.TAIWAN);

    public QueryAssistantService(BudgetBookStore store, StatsService stats) {
        this.store = store;
        this.stats = stats;
    }

    public String answer(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            return helpText();
        }

        YearMonth month = YearMonth.now();
        String normalized = q.toLowerCase(Locale.ROOT);

        Optional<String> categoryAnswer = answerCategoryExpense(month, q);
        if (categoryAnswer.isPresent()) {
            return categoryAnswer.get();
        }
        if (containsAny(q, "花最多", "最多錢", "最大支出", "最高支出")) {
            return topExpenseCategory(month);
        }
        if (containsAny(q, "剩多少預算", "剩餘預算", "還剩多少", "預算剩")) {
            return monthlyBudgetRemaining(month);
        }
        if (containsAny(q, "本月支出", "這個月支出", "月支出", "花多少")) {
            return period(month) + "目前支出是 " + money(stats.monthlyExpense(month)) + "。";
        }
        if (containsAny(q, "本月收入", "這個月收入", "月收入", "賺多少")) {
            return period(month) + "目前收入是 " + money(stats.monthlyIncome(month)) + "。";
        }
        if (containsAny(q, "總餘額", "目前餘額", "還有多少錢", "全部錢包")) {
            return "目前所有錢包合計餘額是 " + money(stats.totalBalance()) + "。";
        }
        if (containsAny(q, "幾個錢包", "錢包數", "帳戶數", "帳戶有幾個")) {
            return walletSummary();
        }
        if (containsAny(q, "小樹", "種樹", "澆水", "樹狀態")) {
            return treeSummary(month);
        }
        if (containsAny(q, "最近一筆", "最新一筆", "最後一筆")) {
            return recentTransaction();
        }
        if (containsAny(q, "交易幾筆", "記帳幾筆", "本月幾筆") || normalized.contains("count")) {
            return period(month) + "目前已記帳 " + stats.monthlyWaterCount(month) + " 筆。";
        }

        return "我目前沒有判斷出這個問題要查哪一項。\n\n" + helpText();
    }

    private Optional<String> answerCategoryExpense(YearMonth month, String question) {
        Map<String, Double> byCategory = stats.monthlyExpenseByCategory(month);
        for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            if (category != null && !category.isBlank() && question.contains(category)) {
                return Optional.of(period(month) + category + "支出是 " + money(entry.getValue()) + "。");
            }
        }
        return Optional.empty();
    }

    private String topExpenseCategory(YearMonth month) {
        return stats.monthlyExpenseByCategory(month).entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(entry -> period(month) + "花最多的分類是「" + entry.getKey() + "」，共 " + money(entry.getValue()) + "。")
                .orElse(period(month) + "目前還沒有支出紀錄。");
    }

    private String monthlyBudgetRemaining(YearMonth month) {
        double limit = store.getSettings().getMonthlyLimit();
        double expense = stats.monthlyExpense(month);
        if (limit <= 0) {
            return "目前尚未設定每月預算上限，可以到設定頁面補上預算。";
        }

        double remaining = limit - expense;
        if (remaining >= 0) {
            return period(month) + "預算是 " + money(limit) + "，已支出 " + money(expense)
                    + "，還剩 " + money(remaining) + "。";
        }
        return period(month) + "預算是 " + money(limit) + "，已支出 " + money(expense)
                + "，目前超出 " + money(Math.abs(remaining)) + "。";
    }

    private String walletSummary() {
        List<Account> accounts = store.getAccounts();
        if (accounts.isEmpty()) {
            return "目前還沒有建立錢包。";
        }

        StringBuilder builder = new StringBuilder("目前有 ")
                .append(accounts.size())
                .append(" 個錢包：");
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            builder.append("\n")
                    .append(i + 1)
                    .append(". ")
                    .append(account.getName())
                    .append("：")
                    .append(money(store.getAccountBalance(account.getId())));
        }
        return builder.toString();
    }

    private String treeSummary(YearMonth month) {
        int waterCount = stats.monthlyWaterCount(month);
        int goal = StatsService.MONTHLY_TREE_GOAL;
        int remaining = Math.max(0, goal - waterCount);
        String status = stats.monthlyTreeStatus(month);
        if (remaining == 0) {
            return period(month) + "小樹狀態：" + status + "。本月已澆水 " + waterCount + " 次，已完成一棵樹。";
        }
        return period(month) + "小樹狀態：" + status + "。本月已澆水 " + waterCount
                + " 次，距離完成一棵樹還差 " + remaining + " 次。";
    }

    private String recentTransaction() {
        List<Transaction> transactions = store.getTransactions();
        if (transactions.isEmpty()) {
            return "目前還沒有任何交易紀錄。";
        }

        Transaction transaction = transactions.get(0);
        Account account = store.findAccount(transaction.getAccountId());
        String kind = transaction.getKind() == Transaction.Kind.INCOME ? "收入" : "支出";
        String accountName = account == null ? "未知錢包" : account.getName();
        return "最近一筆是 " + transaction.getDate().atTime(transaction.getTime()).format(RECENT_TIME_FORMAT)
                + " 的" + kind + "「" + transaction.getCategory() + "」，金額 "
                + money(transaction.getAmount()) + "，使用錢包：" + accountName + "。";
    }

    private String helpText() {
        return "可以問我這些問題：\n"
                + "1. 本月花最多的是哪個分類？\n"
                + "2. 本月支出多少？\n"
                + "3. 我還剩多少預算？\n"
                + "4. 目前總餘額是多少？\n"
                + "5. 我有幾個錢包？\n"
                + "6. 本月小樹狀態？\n"
                + "7. 最近一筆交易是什麼？";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String period(YearMonth month) {
        return month.getYear() + " 年 " + month.getMonthValue() + " 月";
    }

    private String money(double value) {
        return currency.format(value);
    }
}
