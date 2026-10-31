package budgetbook.model;

import budgetbook.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueObjectsTest {
    @Test
    void savingsGoalProgressAndMonthlyNeed() {
        SavingsGoal goal = new SavingsGoal(1, "日本旅行", Money.of(60000), Money.of(24000),
                LocalDate.of(2027, 3, 31), 0);
        assertEquals(40, goal.percent());
        assertEquals(Money.of(36000), goal.remaining());
        assertEquals(6L, goal.monthsLeft(LocalDate.of(2026, 10, 15)).orElseThrow());
        assertEquals(Money.of(6000), goal.monthlyNeeded(LocalDate.of(2026, 10, 15)).orElseThrow());
        assertTrue(goal.deposit(Money.of(36000)).isReached());
        assertThrows(ValidationException.class, () -> goal.withdraw(Money.of(30000)));
        assertEquals(Money.of(14000), goal.withdraw(Money.of(10000)).saved());
    }

    @Test
    void treeStagesFollowOriginalRules() {
        assertEquals(TreeStage.WITHERED, TreeStage.of(0, 20));
        assertEquals(TreeStage.SEED, TreeStage.of(1, 20));
        assertEquals(TreeStage.SPROUT, TreeStage.of(5, 20));
        assertEquals(TreeStage.SAPLING, TreeStage.of(10, 20));
        assertEquals(TreeStage.YOUNG_TREE, TreeStage.of(19, 20));
        assertEquals(TreeStage.FULL_TREE, TreeStage.of(20, 20));
        assertEquals("成長中", TreeStage.SAPLING.statusText());
        MonthlyTree tree = new MonthlyTree(YearMonth.of(2026, 10), 15, 20);
        assertEquals(5, tree.remaining());
        assertFalse(tree.isCompleted());
    }

    @Test
    void dateRangeHelpers() {
        DateRange october = DateRange.ofMonth(YearMonth.of(2026, 10));
        assertEquals(31, october.days());
        assertTrue(october.isFullMonth());
        assertEquals(DateRange.ofMonth(YearMonth.of(2026, 9)), october.previous());
        assertEquals("2026 年 10 月", october.label());
        assertEquals(DateRange.ofYear(2025), DateRange.ofYear(2026).previous());
        DateRange week = DateRange.lastDays(LocalDate.of(2026, 10, 15), 7);
        assertEquals(LocalDate.of(2026, 10, 9), week.start());
        assertEquals(new DateRange(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 8)), week.previous());
        assertThrows(ValidationException.class, () -> DateRange.of(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void budgetStatusLevels() {
        Budget budget = Budget.overall(Money.of(1000));
        YearMonth month = YearMonth.of(2026, 10);
        assertEquals(BudgetLevel.OK, new BudgetStatus(budget, null, month, Money.of(500)).level());
        assertEquals(BudgetLevel.WARNING, new BudgetStatus(budget, null, month, Money.of(800)).level());
        assertEquals(BudgetLevel.WARNING, new BudgetStatus(budget, null, month, Money.of(1000)).level());
        BudgetStatus over = new BudgetStatus(budget, null, month, Money.of(1200));
        assertEquals(BudgetLevel.OVER, over.level());
        assertEquals(Money.of(-200), over.remaining());
        assertEquals(120, over.percent());
        assertEquals("每月總預算", over.label());
    }

    @Test
    void categoryAndWalletValidation() {
        Category category = Category.create(" 餐飲 ", CategoryKind.EXPENSE, 0x1FFFFFF, "");
        assertEquals("餐飲", category.name());
        assertEquals("餐", category.icon());
        assertEquals("#FFFFFF", category.colorHex());
        assertEquals("☕️", Category.create("咖啡", CategoryKind.EXPENSE, 0, "☕️啡").icon());
        assertThrows(ValidationException.class, () -> Category.create("", CategoryKind.EXPENSE, 0, ""));
        assertThrows(ValidationException.class, () -> Wallet.create(" ", WalletType.CASH, Money.ZERO, 0));
        assertEquals(0x4C9A62, Colors.parseHex("#4c9a62"));
        assertThrows(ValidationException.class, () -> Colors.parseHex("green"));
    }
}
