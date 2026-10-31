package budgetbook.model;

import java.util.List;

/**
 * 森林：從第一筆記帳的月份到本月，每個月一棵樹。
 *
 * @param trees          依時間排序，最後一棵是本月
 * @param currentStreak  到今天為止連續有記帳的天數
 * @param longestStreak  歷史最長連續記帳天數
 */
public record Forest(List<MonthlyTree> trees, int currentStreak, int longestStreak) {
    public Forest {
        trees = List.copyOf(trees);
    }

    public long completedCount() {
        return trees.stream().filter(MonthlyTree::isCompleted).count();
    }

    public long witheredCount() {
        return trees.stream().filter(tree -> tree.records() == 0).count();
    }

    public int totalRecords() {
        return trees.stream().mapToInt(MonthlyTree::records).sum();
    }
}
