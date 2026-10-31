package budgetbook.service.report;

import budgetbook.model.DateRange;
import budgetbook.model.PeriodTotals;
import budgetbook.service.StatisticsService;

import java.util.List;

/**
 * Strategy：報表的期間策略。月報與年報的差別（期間、前期、時間序列的粒度）都封裝在這裡，
 * {@link ReportService} 用同一套流程產生任何一種報表。
 */
public interface ReportPeriod {
    /** 例如「2026 年 10 月 月報」。 */
    String title();

    DateRange range();

    /** 用來比較的上一期。 */
    ReportPeriod previous();

    /** 時間序列：月報為每日、年報為每月。 */
    List<PeriodTotals> series(StatisticsService statistics);

    /** 時間序列欄位的名稱，例如「日期」或「月份」。 */
    String seriesLabel();

    /** 月報才有預算比較（預算是每月的）。 */
    boolean includesBudgets();
}
