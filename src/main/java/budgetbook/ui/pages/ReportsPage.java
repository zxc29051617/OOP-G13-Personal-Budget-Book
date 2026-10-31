package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.CategoryTotal;
import budgetbook.model.DateRange;
import budgetbook.model.PeriodSummary;
import budgetbook.ui.DataActions;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.ChartView;
import budgetbook.ui.components.Charts;
import budgetbook.ui.components.PeriodPicker;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.StatTile;
import budgetbook.ui.components.Tables;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Palette;
import budgetbook.service.report.CsvReportExporter;
import budgetbook.service.report.HtmlReportExporter;
import budgetbook.service.report.MonthlyPeriod;
import budgetbook.service.report.Report;
import budgetbook.service.report.ReportPeriod;
import budgetbook.service.report.YearlyPeriod;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;

/** 月報與年報：收支摘要、分類占比、時間序列與資產趨勢，可以匯出 CSV 或 HTML。 */
public final class ReportsPage extends Page {
    private final PeriodPicker picker;
    private final JToggleButton monthly = new JToggleButton("月報", true);
    private final JToggleButton yearly = new JToggleButton("年報");
    private final StatTile income = new StatTile("收入", Palette::income);
    private final StatTile expense = new StatTile("支出", Palette::expense);
    private final StatTile net = new StatTile("結餘", Palette::accent);
    private final StatTile rate = new StatTile("儲蓄率", Palette::text);
    private final ChartView donut = new ChartView(260);
    private final ChartView series = new ChartView(230);
    private final ChartView trend = new ChartView(230);
    private final DefaultTableModel categoryModel = new DefaultTableModel(new Object[]{"分類", "金額", "筆數", "占比"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Tables.Labeled.class : Object.class;
        }
    };
    private final javax.swing.JLabel seriesTitle = Widgets.heading("每日支出");
    private Report report;

    public ReportsPage(UiContext ui) {
        super(ui);
        picker = new PeriodPicker(YearMonth.now(ui.app().clock()));
        picker.addListener(value -> refresh());
        ButtonGroup modes = new ButtonGroup();
        modes.add(monthly);
        modes.add(yearly);
        monthly.addActionListener(e -> picker.setMode(PeriodPicker.Mode.MONTH));
        yearly.addActionListener(e -> picker.setMode(PeriodPicker.Mode.YEAR));
        add(header("依期間彙整收支，並和上一期比較", Widgets.row(4, monthly, yearly), picker,
                Widgets.button("匯出 CSV", () -> DataActions.exportReport(ui, report, new CsvReportExporter())),
                Widgets.button("匯出 HTML", () -> DataActions.exportReport(ui, report, new HtmlReportExporter()))),
                BorderLayout.NORTH);

        ScrollablePanel body = new ScrollablePanel(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(income);
        tiles.add(expense);
        tiles.add(net);
        tiles.add(rate);
        body.add(left(tiles));
        body.add(Box.createVerticalStrut(14));

        JTable table = new JTable(categoryModel);
        Tables.style(table);
        table.getColumnModel().getColumn(1).setCellRenderer(Tables.moneyRenderer(null, false));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        tableScroll.setPreferredSize(new Dimension(360, 260));
        body.add(left(pair(Card.titled("支出分類占比", donut), Card.titled("支出分類明細", tableScroll))));
        body.add(Box.createVerticalStrut(14));
        Card seriesCard = new Card();
        seriesCard.add(seriesTitle, BorderLayout.NORTH);
        seriesCard.add(series, BorderLayout.CENTER);
        body.add(left(pair(seriesCard, Card.titled("資產趨勢", trend))));
        add(Widgets.scroll(body), BorderLayout.CENTER);
    }

    private static JComponent pair(JComponent a, JComponent b) {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.add(a);
        row.add(b);
        return row;
    }

    private static JComponent left(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    @Override
    public String id() {
        return "reports";
    }

    @Override
    public String title() {
        return "報表";
    }

    @Override
    public String glyph() {
        return "報";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.TRANSACTIONS, Topic.CATEGORIES, Topic.BUDGETS, Topic.WALLETS);
    }

    @Override
    protected void reload() {
        YearMonth value = picker.value();
        ReportPeriod period = picker.mode() == PeriodPicker.Mode.MONTH
                ? new MonthlyPeriod(value) : new YearlyPeriod(value.getYear());
        report = ui.app().reports().generate(period);
        PeriodSummary s = report.summary();
        PeriodSummary p = report.previousSummary();
        String previousLabel = period.previous().range().label();
        income.setValue(s.income().format());
        income.setCaption(previousLabel + " " + p.income().format());
        expense.setValue(s.expense().format());
        expense.setCaption(report.expenseChange().map(change -> "比上期" + (change >= 0 ? "多 " : "少 ")
                + Math.abs(Math.round(change * 100)) + "%").orElse(previousLabel + " " + p.expense().format()));
        net.setValue(s.net().format());
        net.setValueColor(s.net().isNegative() ? Palette::expense : Palette::accent);
        net.setCaption("共 " + s.transactionCount() + " 筆交易");
        rate.setValue(s.income().isPositive() ? Math.round(s.savingsRate() * 100) + "%" : "—");
        rate.setCaption("結餘 ÷ 收入");

        List<CategoryTotal> expenses = report.expenseByCategory();
        if (expenses.isEmpty()) {
            donut.showEmpty("這段期間沒有支出");
        } else {
            donut.setChart(Charts.donut(expenses, 8));
        }
        categoryModel.setRowCount(0);
        for (CategoryTotal total : expenses) {
            categoryModel.addRow(new Object[]{new Tables.Labeled(BadgeIcon.of(total.category(), 22),
                    total.category().name()), total.total(), total.count(), total.sharePercent() + "%"});
        }
        boolean isMonthly = period instanceof MonthlyPeriod;
        seriesTitle.setText(isMonthly ? "每日支出" : "每月收入與支出");
        series.setChart(isMonthly ? Charts.expenseBars(report.series()) : Charts.incomeExpenseBars(report.series()));
        LocalDate today = LocalDate.now(ui.app().clock());
        DateRange range = period.range();
        DateRange shown = range.start().isAfter(today) ? range
                : DateRange.of(range.start(), range.end().isAfter(today) ? today : range.end());
        trend.setChart(Charts.balanceLine(ui.app().statistics().balanceTrend(shown)));
    }
}
