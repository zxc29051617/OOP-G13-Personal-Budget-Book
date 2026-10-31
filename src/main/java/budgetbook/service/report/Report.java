package budgetbook.service.report;

import budgetbook.model.BudgetStatus;
import budgetbook.model.CategoryTotal;
import budgetbook.model.Money;
import budgetbook.model.PeriodSummary;
import budgetbook.model.PeriodTotals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 產生好的報表內容（不可變），可以交給不同的 {@link ReportExporter} 輸出。 */
public record Report(ReportPeriod period, PeriodSummary summary, PeriodSummary previousSummary,
                     List<CategoryTotal> expenseByCategory, List<CategoryTotal> incomeByCategory,
                     List<PeriodTotals> series, List<BudgetStatus> budgets, List<Item> topExpenses,
                     LocalDateTime generatedAt) {
    public Report {
        expenseByCategory = List.copyOf(expenseByCategory);
        incomeByCategory = List.copyOf(incomeByCategory);
        series = List.copyOf(series);
        budgets = List.copyOf(budgets);
        topExpenses = List.copyOf(topExpenses);
    }

    /** 報表中的單筆交易（已轉成顯示用的名稱）。 */
    public record Item(LocalDate date, String category, String wallet, String note, Money amount) {
    }

    public String title() {
        return period.title();
    }

    /** 支出與上期相比的變化比例；上期沒有支出時為空。 */
    public Optional<Double> expenseChange() {
        Money before = previousSummary.expense();
        if (before.isZero()) {
            return Optional.empty();
        }
        return Optional.of(summary.expense().minus(before).ratioOf(before));
    }
}
