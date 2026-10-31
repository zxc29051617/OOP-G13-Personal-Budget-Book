package budgetbook.model;

/** 一段期間的收支總結。 */
public record PeriodSummary(DateRange range, Money income, Money expense, int transactionCount) {
    public Money net() {
        return income.minus(expense);
    }

    /** 儲蓄率 = 結餘 / 收入；沒有收入時為 0。 */
    public double savingsRate() {
        return income.isPositive() ? net().ratioOf(income) : 0;
    }
}
