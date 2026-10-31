package budgetbook.service;

import budgetbook.Fixtures;
import budgetbook.model.BalancePoint;
import budgetbook.model.CategoryKind;
import budgetbook.model.CategoryTotal;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.PeriodSummary;
import budgetbook.model.PeriodTotals;
import budgetbook.service.report.CsvReportExporter;
import budgetbook.service.report.HtmlReportExporter;
import budgetbook.service.report.MonthlyPeriod;
import budgetbook.service.report.Report;
import budgetbook.service.report.YearlyPeriod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static budgetbook.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsAndReportTest {
    private final AppContext app = Fixtures.app();
    private final YearMonth october = YearMonth.from(TODAY);

    @BeforeEach
    void setUp() {
        Fixtures.income(app, "薪資", 40000, TODAY.withDayOfMonth(5));
        Fixtures.expense(app, "餐飲", 300, TODAY.withDayOfMonth(1), "早午餐");
        Fixtures.expense(app, "餐飲", 700, TODAY.withDayOfMonth(2), "<聚餐>");
        Fixtures.expense(app, "交通", 1000, TODAY.withDayOfMonth(2), "高鐵");
        Fixtures.expense(app, "購物", 500, TODAY.minusMonths(1), "九月");
        app.budgets().setOverall(Money.of(5000));
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void summaryAndCategoryShares() {
        PeriodSummary summary = app.statistics().summary(DateRange.ofMonth(october));
        assertEquals(Money.of(40000), summary.income());
        assertEquals(Money.of(2000), summary.expense());
        assertEquals(Money.of(38000), summary.net());
        assertEquals(0.95, summary.savingsRate(), 1e-9);

        List<CategoryTotal> totals = app.statistics().totalsByCategory(DateRange.ofMonth(october), CategoryKind.EXPENSE);
        assertEquals(List.of("交通", "餐飲"), totals.stream().map(t -> t.category().name()).toList());
        assertEquals(50, totals.get(0).sharePercent());
        assertEquals(2, totals.get(1).count());
    }

    @Test
    void timeSeries() {
        List<PeriodTotals> months = app.statistics().monthlyTotals(october.minusMonths(1), october);
        assertEquals(List.of("9月", "10月"), months.stream().map(PeriodTotals::label).toList());
        assertEquals(Money.of(500), months.get(0).expense());
        List<PeriodTotals> days = app.statistics().dailyTotals(DateRange.ofMonth(october));
        assertEquals(31, days.size());
        assertEquals("10/2", days.get(1).label());
        assertEquals(Money.of(1700), days.get(1).expense());
        assertEquals("25/12", app.statistics().monthlyTotals(YearMonth.of(2025, 12), october).get(0).label());
    }

    @Test
    void balanceTrendEndsAtCurrentTotal() {
        List<BalancePoint> trend = app.statistics().balanceTrend(DateRange.ofMonth(october));
        assertEquals(31, trend.size());
        assertEquals(Money.of(19000 - 500), trend.get(0).balance().plus(Money.of(300)));
        assertEquals(app.wallets().totalBalance(), trend.get(trend.size() - 1).balance());
    }

    @Test
    void averageDailyExpenseCountsDaysSoFar() {
        assertEquals(Money.parse("133.33"), app.statistics().averageDailyExpense(DateRange.ofMonth(october)));
        assertEquals("交通", app.categories().find(app.statistics().largestExpense(DateRange.ofMonth(october))
                .orElseThrow().categoryId()).orElseThrow().name());
    }

    @Test
    void monthlyReportContainsEverything() {
        Report report = app.reports().generate(new MonthlyPeriod(october));
        assertEquals("2026 年 10 月 月報", report.title());
        assertEquals(Money.of(2000), report.summary().expense());
        assertEquals(Money.of(500), report.previousSummary().expense());
        assertEquals(3.0, report.expenseChange().orElseThrow(), 1e-9);
        assertEquals(31, report.series().size());
        assertEquals(1, report.budgets().size());
        assertEquals("高鐵", report.topExpenses().get(0).note());

        String csv = app.reports().export(report, new CsvReportExporter());
        assertTrue(csv.startsWith("﻿報表,2026 年 10 月 月報"));
        assertTrue(csv.contains("支出,2000"));
        assertTrue(csv.contains("交通,1000,1,50%"));
        assertTrue(csv.contains("支出變化,+300%"));
        assertTrue(csv.contains("每月總預算,5000,2000,3000,40%,正常"));

        String html = app.reports().export(report, new HtmlReportExporter());
        assertTrue(html.contains("&lt;聚餐&gt;"));
        assertFalse(html.contains("<聚餐>"));
    }

    @Test
    void yearlyReportUsesMonthlySeries() {
        Report report = app.reports().generate(new YearlyPeriod(2026));
        assertEquals(12, report.series().size());
        assertEquals(Money.of(2500), report.summary().expense());
        assertTrue(report.budgets().isEmpty());
        assertTrue(report.expenseChange().isEmpty(), "no data in 2025");
    }
}
