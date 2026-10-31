package budgetbook.model;

import java.time.YearMonth;

/** 某個月份的小樹：記帳次數、目標與成長階段。 */
public record MonthlyTree(YearMonth month, int records, int goal) {
    public TreeStage stage() {
        return TreeStage.of(records, goal);
    }

    public boolean isCompleted() {
        return records >= goal;
    }

    public double progress() {
        return Math.min(1.0, records / (double) goal);
    }

    public int remaining() {
        return Math.max(0, goal - records);
    }
}
