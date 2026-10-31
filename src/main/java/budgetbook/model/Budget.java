package budgetbook.model;

/**
 * 每月預算。{@code categoryId} 為 null 代表「每月總預算」，否則是單一支出分類的預算。
 * 預算每個月自動套用，不需要每月重新設定。
 */
public record Budget(long id, Long categoryId, Money monthlyLimit) implements Entity {
    public Budget {
        Checks.requirePositive(monthlyLimit, "預算金額");
    }

    public static Budget overall(Money limit) {
        return new Budget(0, null, limit);
    }

    public static Budget forCategory(long categoryId, Money limit) {
        return new Budget(0, categoryId, limit);
    }

    public boolean isOverall() {
        return categoryId == null;
    }

    public Budget withId(long newId) {
        return new Budget(newId, categoryId, monthlyLimit);
    }

    public Budget withLimit(Money limit) {
        return new Budget(id, categoryId, limit);
    }
}
