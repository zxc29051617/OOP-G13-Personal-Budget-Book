package budgetbook.model;

import java.time.LocalDate;

/** 定期交易的週期。 */
public enum Frequency {
    DAILY("每天", "天"),
    WEEKLY("每週", "週"),
    MONTHLY("每月", "個月"),
    YEARLY("每年", "年");

    private final String displayName;
    private final String unit;

    Frequency(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * 第 n 次（從 0 起算）發生的日期。一律從開始日推算而不是從上一次推算，
     * 所以 1/31 開始的每月交易會是 2/28、3/31、4/30，不會越漂越早。
     */
    public LocalDate nth(LocalDate start, int interval, int n) {
        long steps = (long) interval * n;
        return switch (this) {
            case DAILY -> start.plusDays(steps);
            case WEEKLY -> start.plusWeeks(steps);
            case MONTHLY -> start.plusMonths(steps);
            case YEARLY -> start.plusYears(steps);
        };
    }

    /** 例如「每月」「每 2 週」。 */
    public String describe(int interval) {
        return interval <= 1 ? displayName : "每 " + interval + " " + unit;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
