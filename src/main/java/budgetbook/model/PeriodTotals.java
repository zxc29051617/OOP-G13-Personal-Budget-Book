package budgetbook.model;

import java.time.LocalDate;

/** 圖表用的區段統計，例如「10 月」或「10/15」的收入與支出。 */
public record PeriodTotals(String label, LocalDate start, Money income, Money expense) {
    public Money net() {
        return income.minus(expense);
    }
}
