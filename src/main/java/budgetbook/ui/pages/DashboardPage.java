package budgetbook.ui.pages;

import budgetbook.event.Topic;
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
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.service.AppContext;
import budgetbook.service.RecurringService;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.ChartView;
import budgetbook.ui.components.Charts;
import budgetbook.ui.components.ProgressMeter;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.StatTile;
import budgetbook.ui.components.TreeView;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.forms.TransactionForm;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 總覽：本月收支、資產、分類圓餅圖、預算、六個月收支、小樹、最近交易、即將到來的定期交易與儲蓄目標。 */
public final class DashboardPage extends Page {
    private static final int SIDE_WIDTH = 340;
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("M/d");

    private final StatTile assets = new StatTile("總資產", Palette::text);
    private final StatTile income = new StatTile("本月收入", Palette::income);
    private final StatTile expense = new StatTile("本月支出", Palette::expense);
    private final StatTile net = new StatTile("本月結餘", Palette::accent);
    private final ChartView donut = new ChartView(250);
    private final ChartView bars = new ChartView(220);
    private final JPanel budgets = column();
    private final TreeView tree = new TreeView(120);
    private final JLabel treeTitle = Widgets.heading("");
    private final JLabel treeText = Widgets.muted("");
    private final JLabel streakText = Widgets.muted("");
    private final ProgressMeter treeMeter = new ProgressMeter(8);
    private final JPanel recent = column();
    private final JPanel upcoming = column();
    private final JPanel goals = column();
    private final JLabel subtitle = Widgets.muted("");

    public DashboardPage(UiContext ui) {
        super(ui);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Widgets.column(2, Widgets.title(title()), subtitle), BorderLayout.WEST);
        header.add(Widgets.row(8,
                Widgets.button("轉帳", () -> newTransaction(TransactionType.TRANSFER)),
                Widgets.primary("＋ 記一筆", () -> newTransaction(TransactionType.EXPENSE))), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        ScrollablePanel body = new ScrollablePanel(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(assets);
        tiles.add(income);
        tiles.add(expense);
        tiles.add(net);
        body.add(stretch(tiles));
        body.add(Box.createVerticalStrut(14));
        body.add(split(Card.titled("本月支出分類", Widgets.link("報表", () -> ui.navigate("reports")), donut),
                Card.titled("本月預算", Widgets.link("全部預算", () -> ui.navigate("budgets")), budgets)));
        body.add(Box.createVerticalStrut(14));
        body.add(split(Card.titled("近 6 個月收支", bars), treeCard()));
        body.add(Box.createVerticalStrut(14));
        JPanel side = column();
        side.add(Card.titled("接下來 14 天", Widgets.link("定期交易", () -> ui.navigate("recurring")), upcoming));
        side.add(Box.createVerticalStrut(14));
        side.add(Card.titled("儲蓄目標", Widgets.link("全部目標", () -> ui.navigate("goals")), goals));
        body.add(split(Card.titled("最近交易", Widgets.link("全部交易", () -> ui.navigate("transactions")), recent),
                side));
        add(Widgets.scroll(body), BorderLayout.CENTER);
    }

    private JComponent treeCard() {
        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.setOpaque(false);
        content.add(tree, BorderLayout.WEST);
        content.add(Widgets.column(6, treeTitle, treeText, treeMeter, streakText,
                Widgets.link("看看我的森林 →", () -> ui.navigate("forest"))), BorderLayout.CENTER);
        return Card.titled("本月小樹", content);
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JComponent stretch(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    private static JComponent split(JComponent main, JComponent side) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.add(main, BorderLayout.CENTER);
        side.setPreferredSize(new Dimension(SIDE_WIDTH, side.getPreferredSize().height));
        row.add(side, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void newTransaction(TransactionType type) {
        ui.open(new TransactionForm(ui.app(), null, type));
    }

    @Override
    public String id() {
        return "dashboard";
    }

    @Override
    public String title() {
        return "總覽";
    }

    @Override
    public String glyph() {
        return "覽";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.allOf(Topic.class);
    }

    @Override
    protected void reload() {
        AppContext app = ui.app();
        LocalDate today = app.statistics().today();
        YearMonth month = YearMonth.from(today);
        subtitle.setText(month.getYear() + " 年 " + month.getMonthValue() + " 月 · 今天是 " + today.format(SHORT)
                + "（" + today.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.TAIWAN) + "）");
        DateRange range = DateRange.ofMonth(month);
        PeriodSummary summary = app.statistics().summary(range);
        PeriodSummary previous = app.statistics().summary(range.previous());

        assets.setValue(app.wallets().totalBalance().format());
        assets.setCaption(app.wallets().active().size() + " 個錢包");
        income.setValue(summary.income().format());
        income.setCaption(compare(summary.income(), previous.income()));
        expense.setValue(summary.expense().format());
        expense.setCaption(compare(summary.expense(), previous.expense()));
        net.setValue(summary.net().format());
        net.setValueColor(summary.net().isNegative() ? Palette::expense : Palette::accent);
        net.setCaption(summary.income().isPositive()
                ? "儲蓄率 " + Math.round(summary.savingsRate() * 100) + "%" : "本月還沒有收入");

        List<CategoryTotal> totals = app.statistics().totalsByCategory(range, CategoryKind.EXPENSE);
        if (totals.isEmpty()) {
            donut.showEmpty("本月還沒有支出");
        } else {
            donut.setChart(Charts.donut(totals, 7));
        }
        bars.setChart(Charts.incomeExpenseBars(app.statistics().monthlyTotals(month.minusMonths(5), month)));

        fillBudgets(app, month);
        fillTree(app);
        fillRecent(app);
        fillUpcoming(app);
        fillGoals(app, today);
    }

    private static String compare(Money now, Money before) {
        if (before.isZero()) {
            return "上月沒有紀錄";
        }
        long percent = Math.round(now.minus(before).ratioOf(before) * 100);
        return percent == 0 ? "與上月相同" : "比上月" + (percent > 0 ? "多 " : "少 ") + Math.abs(percent) + "%";
    }

    private void fillBudgets(AppContext app, YearMonth month) {
        budgets.removeAll();
        List<BudgetStatus> statuses = app.budgets().statuses(month);
        if (statuses.isEmpty()) {
            budgets.add(Widgets.empty("還沒有設定預算"));
            budgets.add(Widgets.link("設定每月預算 →", () -> ui.navigate("budgets")));
            return;
        }
        int shown = 0;
        for (BudgetStatus status : statuses) {
            if (shown++ >= 4) {
                break;
            }
            budgets.add(budgetRow(status));
        }
        Optional<BudgetStatus> overall = statuses.stream().filter(BudgetStatus::isOverall).findFirst();
        overall.ifPresent(status -> budgets.add(Widgets.pad(Widgets.muted("依目前速度，月底預估支出 "
                + app.budgets().projectedSpending(month, status.spent()).format()), 6, 0, 0, 0)));
    }

    static JComponent budgetRow(BudgetStatus status) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 6, 0));
        JPanel labels = new JPanel(new BorderLayout());
        labels.setOpaque(false);
        labels.add(Widgets.label(status.label()), BorderLayout.WEST);
        JLabel percent = Widgets.value(status.percent() + "%", 0.93f, () -> Palette.forLevel(status.level()));
        labels.add(percent, BorderLayout.EAST);
        row.add(labels, BorderLayout.NORTH);
        ProgressMeter meter = new ProgressMeter(8);
        meter.setRatio(status.ratio());
        meter.setColor(() -> Palette.forLevel(status.level()));
        row.add(meter, BorderLayout.CENTER);
        String detail = status.spent().format() + " / " + status.limit().format() + " · "
                + (status.remaining().isNegative() ? "超出 " + status.remaining().abs().format()
                : "剩 " + status.remaining().format());
        row.add(Widgets.muted(detail), BorderLayout.SOUTH);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void fillTree(AppContext app) {
        MonthlyTree current = app.trees().currentTree();
        Forest forest = app.trees().forest(24);
        tree.setStage(current.stage());
        treeTitle.setText(current.stage().displayName() + "（" + current.stage().statusText() + "）");
        treeText.setText("本月澆水 " + current.records() + " / " + current.goal() + " 次"
                + (current.isCompleted() ? "，已經完成！" : "，再 " + current.remaining() + " 次完成"));
        treeMeter.setRatio(current.progress());
        streakText.setText("連續記帳 " + forest.currentStreak() + " 天 · 森林已有 " + forest.completedCount() + " 棵樹");
    }

    private void fillRecent(AppContext app) {
        recent.removeAll();
        List<Transaction> rows = app.transactions().recent(7);
        if (rows.isEmpty()) {
            recent.add(Widgets.empty("還沒有交易，按「＋ 記一筆」或 Ctrl+N 開始記帳"));
            return;
        }
        Map<Long, Category> categories = app.categories().byId();
        Map<Long, Wallet> wallets = app.transactions().walletsById();
        rows.forEach(t -> recent.add(Rows.transaction(t, categories, wallets)));
    }

    private void fillUpcoming(AppContext app) {
        upcoming.removeAll();
        List<RecurringService.Occurrence> items = app.recurring().upcoming(14);
        if (items.isEmpty()) {
            upcoming.add(Widgets.empty("沒有即將到來的定期交易"));
            return;
        }
        items.stream().limit(5).forEach(o -> {
            String when = o.date().format(SHORT) + "（" + o.date().getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.TAIWAN) + "）";
            String amount = (o.rule().type() == TransactionType.INCOME ? "+"
                    : o.rule().type() == TransactionType.EXPENSE ? "-" : "") + o.rule().amount().format();
            upcoming.add(Rows.row(new BadgeIcon(o.rule().type() == TransactionType.TRANSFER ? "轉" : "定",
                    Palette.forType(o.rule().type()), 28), o.rule().name(), when + " · " + o.rule().scheduleText(),
                    amount, () -> Palette.forType(o.rule().type())));
        });
    }

    private void fillGoals(AppContext app, LocalDate today) {
        goals.removeAll();
        List<SavingsGoal> list = app.goals().all();
        if (list.isEmpty()) {
            goals.add(Widgets.empty("設定一個存錢目標吧"));
            return;
        }
        list.stream().limit(3).forEach(goal -> {
            JPanel row = new JPanel(new BorderLayout(0, 4));
            row.setOpaque(false);
            row.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 6, 0));
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(Widgets.label(goal.name()), BorderLayout.WEST);
            top.add(Widgets.muted(goal.saved().format() + " / " + goal.target().format()), BorderLayout.EAST);
            row.add(top, BorderLayout.NORTH);
            ProgressMeter meter = new ProgressMeter(8);
            meter.setRatio(goal.progress());
            meter.setColor(() -> Palette.of(goal.color()));
            row.add(meter, BorderLayout.CENTER);
            row.add(Widgets.muted(goal.isReached() ? "已達成！" : goal.monthlyNeeded(today)
                    .map(m -> "每月需存 " + m.format()).orElse("還差 " + goal.remaining().format())), BorderLayout.SOUTH);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            goals.add(row);
        });
    }
}
