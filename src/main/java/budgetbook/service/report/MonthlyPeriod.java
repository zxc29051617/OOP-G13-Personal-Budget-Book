package budgetbook.service.report;

import budgetbook.model.DateRange;
import budgetbook.model.PeriodTotals;
import budgetbook.service.StatisticsService;

import java.time.YearMonth;
import java.util.List;

/** 月報：一個月，時間序列為每日收支。 */
public record MonthlyPeriod(YearMonth month) implements ReportPeriod {
    @Override
    public String title() {
        return month.getYear() + " 年 " + month.getMonthValue() + " 月 月報";
    }

    @Override
    public DateRange range() {
        return DateRange.ofMonth(month);
    }

    @Override
    public ReportPeriod previous() {
        return new MonthlyPeriod(month.minusMonths(1));
    }

    @Override
    public List<PeriodTotals> series(StatisticsService statistics) {
        return statistics.dailyTotals(range());
    }

    @Override
    public String seriesLabel() {
        return "日期";
    }

    @Override
    public boolean includesBudgets() {
        return true;
    }
}
