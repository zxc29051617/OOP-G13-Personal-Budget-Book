package budgetbook.service.assistant;

import budgetbook.model.BudgetLevel;
import budgetbook.model.BudgetStatus;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.CategoryTotal;
import budgetbook.model.DateRange;
import budgetbook.model.Forest;
import budgetbook.model.MonthlyTree;
import budgetbook.model.Money;
import budgetbook.model.PeriodSummary;
import budgetbook.model.SavingsGoal;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.Wallet;
import budgetbook.service.RecurringService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** 各種問題的實際回答；每個方法對應一種意圖，答案都由 service 從資料庫算出。 */
final class Answers {
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d");

    private final AssistantServices s;

    Answers(AssistantServices services) {
        this.s = services;
    }

    private DateRange thisMonth() {
        return DateRange.ofMonth(YearMonth.from(s.today()));
    }

    String compare(Question q) {
        DateRange range = q.rangeOr(thisMonth());
        String label = q.labelOr("本月");
        // 「跟上個月比」：提到的是比較基準，主角是本月（或今年）
        DateRange present = range.isFullMonth() ? thisMonth()
                : range.isFullYear() ? DateRange.ofYear(s.today().getYear()) : range;
        if (!present.equals(range) && present.previous().equals(range)) {
            label = range.isFullMonth() ? "本月" : "今年";
            range = present;
        }
        DateRange previous = range.previous();
        String previousLabel = previous.label();
        if (q.category() != null) {
            Money now = categoryTotal(q.category(), range);
            Money before = categoryTotal(q.category(), previous);
            return label + "「" + q.category().name() + "」" + q.category().kind().displayName() + " " + now.format()
                    + "，" + previousLabel + "是 " + before.format() + "，" + difference(now, before) + "。";
        }
        PeriodSummary current = s.statistics().summary(range);
        PeriodSummary last = s.statistics().summary(previous);
        String text = label + "支出 " + current.expense().format() + "，" + previousLabel + "是 "
                + last.expense().format() + "，" + difference(current.expense(), last.expense()) + "。\n"
                + label + "收入 " + current.income().format() + "，" + previousLabel + "是 "
                + last.income().format() + "，" + difference(current.income(), last.income()) + "。";
        if (range.contains(s.today()) && range.end().isAfter(s.today())) {
            text += "\n（" + label + "還沒結束，數字會再變動。）";
        }
        return text;
    }

    private static String difference(Money now, Money before) {
        Money diff = now.minus(before);
        if (diff.isZero()) {
            return "兩者相同";
        }
        String direction = diff.isPositive() ? "多了 " : "少了 ";
        String percent = before.isZero() ? ""
                : String.format(Locale.ROOT, "（%+d%%）", Math.round(diff.ratioOf(before) * 100));
        return direction + diff.abs().format() + percent;
    }

    String budget(Question q) {
        YearMonth month = YearMonth.from(q.rangeOr(thisMonth()).start());
        List<BudgetStatus> statuses = s.budgets().statuses(month);
        String label = month.equals(YearMonth.from(s.today())) ? "本月" : month.getYear() + " 年 " + month.getMonthValue() + " 月";
        if (q.category() != null) {
            return statuses.stream()
                    .filter(st -> st.category() != null && st.category().id() == q.category().id())
                    .findFirst()
                    .map(st -> label + "「" + st.label() + "」預算 " + st.limit().format() + "，已用 " + st.spent().format()
                            + "（" + st.percent() + "%），" + remainingText(st) + "。")
                    .orElse("「" + q.category().name() + "」還沒有設定預算，可以到「預算」頁設定。");
        }
        if (statuses.isEmpty()) {
            return "還沒有設定任何預算，可以到「預算」頁設定每月總預算或分類預算。";
        }
        StringBuilder text = new StringBuilder();
        Optional<BudgetStatus> overall = statuses.stream().filter(BudgetStatus::isOverall).findFirst();
        overall.ifPresent(st -> {
            text.append(label).append("總預算 ").append(st.limit().format()).append("，已用 ").append(st.spent().format())
                    .append("（").append(st.percent()).append("%），").append(remainingText(st)).append("。");
            if (month.equals(YearMonth.from(s.today()))) {
                text.append("\n依目前速度，月底預估支出 ")
                        .append(s.budgets().projectedSpending(month, st.spent()).format()).append("。");
            }
        });
        List<BudgetStatus> alerts = statuses.stream()
                .filter(st -> !st.isOverall() && st.level() != BudgetLevel.OK).toList();
        if (!alerts.isEmpty()) {
            text.append(text.isEmpty() ? "" : "\n").append("需要注意：");
            for (BudgetStatus st : alerts) {
                text.append("\n・").append(st.label()).append(" ").append(st.percent()).append("%（")
                        .append(st.level().displayName()).append("，").append(remainingText(st)).append("）");
            }
        } else if (statuses.stream().anyMatch(st -> !st.isOverall())) {
            text.append(text.isEmpty() ? "" : "\n").append("各分類預算都在 80% 以內。");
        }
        return text.toString();
    }

    private static String remainingText(BudgetStatus status) {
        Money remaining = status.remaining();
        return remaining.isNegative() ? "超出 " + remaining.abs().format() : "還剩 " + remaining.format();
    }

    String topCategories(Question q) {
        DateRange range = q.rangeOr(thisMonth());
        CategoryKind kind = q.has("收入", "賺") ? CategoryKind.INCOME : CategoryKind.EXPENSE;
        List<CategoryTotal> totals = s.statistics().totalsByCategory(range, kind);
        String label = q.labelOr("本月");
        if (totals.isEmpty()) {
            return label + "還沒有" + kind.displayName() + "紀錄。";
        }
        CategoryTotal top = totals.get(0);
        StringBuilder text = new StringBuilder(label).append(kind.displayName()).append("最多的是「")
                .append(top.category().name()).append("」，共 ").append(top.total().format()).append("，占 ")
                .append(top.sharePercent()).append("%。");
        if (totals.size() > 1) {
            text.append("\n前三名：");
            for (int i = 0; i < Math.min(3, totals.size()); i++) {
                CategoryTotal t = totals.get(i);
                text.append("\n").append(i + 1).append(". ").append(t.category().name()).append(" ")
                        .append(t.total().format()).append("（").append(t.sharePercent()).append("%）");
            }
        }
        return text.toString();
    }

    String largestExpense(Question q) {
        DateRange range = q.rangeOr(thisMonth());
        return s.statistics().largestExpense(range)
                .map(t -> q.labelOr("本月") + "最大的一筆支出是 " + describe(t) + "。")
                .orElse(q.labelOr("本月") + "還沒有支出紀錄。");
    }

    String goals(Question q) {
        List<SavingsGoal> goals = q.goal() != null ? List.of(q.goal()) : s.goals().all();
        if (goals.isEmpty()) {
            return "還沒有儲蓄目標，可以到「目標」頁新增一個，例如旅行基金。";
        }
        StringBuilder text = new StringBuilder();
        for (SavingsGoal goal : goals) {
            if (!text.isEmpty()) {
                text.append("\n");
            }
            text.append("「").append(goal.name()).append("」已存 ").append(goal.saved().format()).append(" / ")
                    .append(goal.target().format()).append("（").append(goal.percent()).append("%）");
            if (goal.isReached()) {
                text.append("，已經達成！");
                continue;
            }
            text.append("，還差 ").append(goal.remaining().format());
            goal.monthlyNeeded(s.today()).ifPresent(monthly -> text.append("；要在 ").append(goal.deadline())
                    .append(" 前達成，每月約需存 ").append(monthly.format()));
            text.append("。");
        }
        return text.toString();
    }

    String recurring(Question q) {
        List<RecurringService.Occurrence> upcoming = s.recurring().upcoming(30);
        if (upcoming.isEmpty()) {
            return "接下來 30 天沒有定期交易。可以到「定期」頁新增房租、薪資或訂閱。";
        }
        StringBuilder text = new StringBuilder("接下來 30 天有 ").append(upcoming.size()).append(" 筆定期交易：");
        upcoming.stream().limit(8).forEach(o -> text.append("\n・").append(o.date().format(SHORT_DATE)).append(" ")
                .append(o.rule().name()).append(" ").append(o.rule().amount().format()).append("（")
                .append(o.rule().type().displayName()).append("）"));
        if (upcoming.size() > 8) {
            text.append("\n……還有 ").append(upcoming.size() - 8).append(" 筆");
        }
        return text.toString();
    }

    String tree(Question q) {
        MonthlyTree tree = s.trees().currentTree();
        Forest forest = s.trees().forest(24);
        StringBuilder text = new StringBuilder("本月已澆水 ").append(tree.records()).append(" / ").append(tree.goal())
                .append(" 次，小樹狀態：").append(tree.stage().label());
        text.append(tree.isCompleted() ? "，本月的樹已經完成！" : "，再記 " + tree.remaining() + " 筆就完成一棵樹。");
        text.append("\n目前連續記帳 ").append(forest.currentStreak()).append(" 天，最長紀錄 ")
                .append(forest.longestStreak()).append(" 天；森林裡已有 ").append(forest.completedCount())
                .append(" 棵完成的樹。");
        return text.toString();
    }

    String category(Question q) {
        Category category = q.category();
        DateRange range = q.rangeOr(thisMonth());
        List<Transaction> rows = s.transactions().search(TransactionQuery.all().withRange(range)
                .withCategory(category.id()));
        Money total = Money.sum(rows.stream().map(Transaction::amount));
        String label = q.labelOr("本月");
        StringBuilder text = new StringBuilder(label).append("「").append(category.name()).append("」")
                .append(category.kind().displayName()).append(" ").append(total.format()).append("，共 ")
                .append(rows.size()).append(" 筆");
        if (category.kind() == CategoryKind.EXPENSE && total.isPositive()) {
            Money all = s.statistics().summary(range).expense();
            text.append("，占總支出 ").append(Math.round(total.ratioOf(all) * 100)).append("%");
        }
        text.append("。");
        if (category.kind() == CategoryKind.EXPENSE && range.isFullMonth()) {
            s.budgets().forCategory(category.id()).ifPresent(b -> text.append("\n預算 ").append(b.monthlyLimit().format())
                    .append("，已用 ").append(Math.round(total.ratioOf(b.monthlyLimit()) * 100)).append("%。"));
        }
        return text.toString();
    }

    String wallet(Question q) {
        Wallet wallet = q.wallet();
        Money balance = s.wallets().balanceOf(wallet.id());
        DateRange range = q.rangeOr(thisMonth());
        List<Transaction> rows = s.transactions().search(TransactionQuery.all().withRange(range)
                .withWallet(wallet.id()));
        Money in = Money.ZERO;
        Money out = Money.ZERO;
        for (Transaction t : rows) {
            Money effect = t.balanceEffectOn(wallet.id());
            if (effect.isPositive()) {
                in = in.plus(effect);
            } else {
                out = out.plus(effect.abs());
            }
        }
        return "「" + wallet.name() + "」目前餘額 " + balance.format() + "。\n" + q.labelOr("本月") + "流入 " + in.format()
                + "、流出 " + out.format() + "（共 " + rows.size() + " 筆）。";
    }

    String walletList() {
        Map<Long, Money> balances = s.wallets().balances();
        List<Wallet> wallets = s.wallets().all();
        if (wallets.isEmpty()) {
            return "還沒有錢包，可以到「錢包」頁新增。";
        }
        StringBuilder text = new StringBuilder("目前有 ").append(wallets.size()).append(" 個錢包：");
        for (Wallet wallet : wallets) {
            text.append("\n・").append(wallet.name()).append(" ").append(balances.get(wallet.id()).format());
            if (wallet.archived()) {
                text.append("（已封存）");
            }
        }
        return text.append("\n合計 ").append(s.wallets().totalBalance().format()).append("。").toString();
    }

    String balance() {
        Money total = s.wallets().totalBalance();
        Money earmarked = s.goals().totalSaved();
        StringBuilder text = new StringBuilder("目前所有錢包合計 ").append(total.format()).append("。");
        if (earmarked.isPositive()) {
            text.append("\n其中 ").append(earmarked.format()).append(" 已標記給儲蓄目標。");
        }
        return text.toString();
    }

    String average(Question q) {
        DateRange range = q.rangeOr(thisMonth());
        return q.labelOr("本月") + "平均每天支出 " + s.statistics().averageDailyExpense(range).format() + "。";
    }

    String net(Question q) {
        PeriodSummary summary = s.statistics().summary(q.rangeOr(thisMonth()));
        return q.labelOr("本月") + "收入 " + summary.income().format() + "、支出 " + summary.expense().format()
                + "，結餘 " + summary.net().format() + "，儲蓄率 " + Math.round(summary.savingsRate() * 100) + "%。";
    }

    String income(Question q) {
        DateRange range = q.rangeOr(thisMonth());
        PeriodSummary summary = s.statistics().summary(range);
        StringBuilder text = new StringBuilder(q.labelOr("本月")).append("收入 ").append(summary.income().format()).append("。");
        List<CategoryTotal> totals = s.statistics().totalsByCategory(range, CategoryKind.INCOME);
        if (!totals.isEmpty()) {
            text.append("主要來自「").append(totals.get(0).category().name()).append("」")
                    .append(totals.get(0).total().format()).append("。");
        }
        return text.toString();
    }

    String expense(Question q) {
        DateRange range = q.rangeOr(thisMonth());
        PeriodSummary summary = s.statistics().summary(range);
        long count = s.transactions().search(TransactionQuery.all().withRange(range)).stream()
                .filter(Transaction::isExpense).count();
        return q.labelOr("本月") + "支出 " + summary.expense().format() + "，共 " + count + " 筆。";
    }

    String count(Question q) {
        DateRange range = q.rangeOr(thisMonth());
        return q.labelOr("本月") + "共記了 " + s.statistics().summary(range).transactionCount() + " 筆。";
    }

    String recent() {
        List<Transaction> recent = s.transactions().recent(3);
        if (recent.isEmpty()) {
            return "還沒有任何交易紀錄。按 Ctrl+N 記下第一筆吧！";
        }
        StringBuilder text = new StringBuilder("最近一筆是 ").append(describe(recent.get(0))).append("。");
        if (recent.size() > 1) {
            text.append("\n再之前：");
            recent.subList(1, recent.size()).forEach(t -> text.append("\n・").append(describe(t)));
        }
        return text.toString();
    }

    private String describe(Transaction t) {
        String date = t.date().equals(s.today()) ? "今天" : t.date().format(SHORT_DATE);
        String what;
        if (t.isTransfer()) {
            what = walletName(t.walletId()) + " 轉到 " + walletName(t.toWalletId());
        } else {
            what = s.categories().find(t.categoryId()).map(Category::name).orElse("未分類")
                    + (t.note().isBlank() ? "" : "「" + t.note() + "」");
        }
        return date + " " + t.time() + " " + t.type().displayName() + " " + what + " " + t.amount().format();
    }

    private String walletName(Long id) {
        return id == null ? "?" : s.wallets().all().stream().filter(w -> w.id() == id).map(Wallet::name)
                .findFirst().orElse("?");
    }

    private Money categoryTotal(Category category, DateRange range) {
        return Money.sum(s.transactions().search(TransactionQuery.all().withRange(range).withCategory(category.id()))
                .stream().map(Transaction::amount));
    }

    /** 方便測試：今天的日期。 */
    LocalDate today() {
        return s.today();
    }
}
