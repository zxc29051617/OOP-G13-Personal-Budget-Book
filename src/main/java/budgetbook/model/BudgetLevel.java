package budgetbook.model;

/** 預算使用狀態：80% 以上提醒，超過 100% 警告。 */
public enum BudgetLevel {
    OK("正常"),
    WARNING("接近上限"),
    OVER("已超支");

    public static final double WARNING_RATIO = 0.8;

    private final String displayName;

    BudgetLevel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static BudgetLevel of(double ratio) {
        if (ratio > 1.0) {
            return OVER;
        }
        return ratio >= WARNING_RATIO ? WARNING : OK;
    }
}
