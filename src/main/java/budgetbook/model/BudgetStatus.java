package budgetbook.model;

import java.time.YearMonth;

/**
 * 某個月份某項預算的使用情況（唯讀的計算結果）。
 *
 * @param category 總預算時為 null
 */
public record BudgetStatus(Budget budget, Category category, YearMonth month, Money spent) {
    public String label() {
        return category == null ? "每月總預算" : category.name();
    }

    public boolean isOverall() {
        return budget.isOverall();
    }

    public Money limit() {
        return budget.monthlyLimit();
    }

    public Money remaining() {
        return limit().minus(spent);
    }

    public double ratio() {
        return spent.ratioOf(limit());
    }

    public int percent() {
        return (int) Math.round(ratio() * 100);
    }

    public BudgetLevel level() {
        return BudgetLevel.of(ratio());
    }
}
