package budgetbook.service.report;

import budgetbook.model.DateRange;
import budgetbook.model.PeriodTotals;
import budgetbook.service.StatisticsService;

import java.time.YearMonth;
import java.util.List;

/** 年報：一整年，時間序列為每月收支。 */
public record YearlyPeriod(int year) implements ReportPeriod {
    @Override
    public String title() {
        return year + " 年 年報";
    }

    @Override
    public DateRange range() {
        return DateRange.ofYear(year);
    }

    @Override
    public ReportPeriod previous() {
        return new YearlyPeriod(year - 1);
    }

    @Override
    public List<PeriodTotals> series(StatisticsService statistics) {
        return statistics.monthlyTotals(YearMonth.of(year, 1), YearMonth.of(year, 12));
    }

    @Override
    public String seriesLabel() {
        return "月份";
    }

    @Override
    public boolean includesBudgets() {
        return false;
    }
}
