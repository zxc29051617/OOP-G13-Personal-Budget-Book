package budgetbook.service;

import budgetbook.Fixtures;
import budgetbook.exception.ValidationException;
import budgetbook.model.BudgetLevel;
import budgetbook.model.BudgetStatus;
import budgetbook.model.CategoryKind;
import budgetbook.model.Forest;
import budgetbook.model.Frequency;
import budgetbook.model.Money;
import budgetbook.model.RecurringRule;
import budgetbook.model.SavingsGoal;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionType;
import budgetbook.model.TreeStage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static budgetbook.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningServicesTest {
    private final AppContext app = Fixtures.app();
    private final YearMonth october = YearMonth.from(TODAY);

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void budgetStatusesAreOrderedWithOverallFirst() {
        long food = Fixtures.category(app, CategoryKind.EXPENSE, "餐飲").id();
        long fun = Fixtures.category(app, CategoryKind.EXPENSE, "娛樂").id();
        app.budgets().setOverall(Money.of(10000));
        app.budgets().setForCategory(food, Money.of(1000));
        app.budgets().setForCategory(fun, Money.of(1000));
        Fixtures.expense(app, "餐飲", 850, TODAY, "");
        Fixtures.expense(app, "娛樂", 1200, TODAY, "");
        Fixtures.expense(app, "娛樂", 500, TODAY.minusMonths(1), "上個月不算");

        List<BudgetStatus> statuses = app.budgets().statuses(october);
        assertEquals(List.of("每月總預算", "娛樂", "餐飲"), statuses.stream().map(BudgetStatus::label).toList());
        assertEquals(BudgetLevel.OVER, statuses.get(1).level());
        assertEquals(BudgetLevel.WARNING, statuses.get(2).level());
        assertEquals(Money.of(2050), statuses.get(0).spent());
        assertEquals(2, app.budgets().alerts(october).size());

        app.budgets().setForCategory(fun, null);
        assertTrue(app.budgets().forCategory(fun).isEmpty());
        assertThrows(ValidationException.class, () -> app.budgets().setForCategory(
                Fixtures.category(app, CategoryKind.INCOME, "薪資").id(), Money.of(5)));
    }

    @Test
    void projectionUsesSpendingPace() {
        // 10/15 已花 1,500，31 天的月份預估 3,100
        assertEquals(Money.of(3100), app.budgets().projectedSpending(october, Money.of(1500)));
        assertEquals(Money.of(1500), app.budgets().projectedSpending(october.minusMonths(1), Money.of(1500)));
    }

    private RecurringRule rent(LocalDate start) {
        return app.recurring().save(new RecurringRule(0, "房租", TransactionType.EXPENSE, Money.of(9500),
                Fixtures.category(app, CategoryKind.EXPENSE, "居住").id(), Fixtures.wallet(app, "銀行帳戶").id(),
                null, "", Frequency.MONTHLY, 1, start, null, 0, true));
    }

    @Test
    void recurringCatchUpPostsEachMissedOccurrenceOnce() {
        RecurringRule rule = rent(LocalDate.of(2026, 8, 1));
        assertEquals(3, app.recurring().dueCount());
        List<Transaction> posted = app.recurring().postDue();
        assertEquals(List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)),
                posted.stream().map(Transaction::date).toList());
        assertTrue(posted.stream().allMatch(t -> t.recurringRuleId() == rule.id()));
        assertEquals(LocalDate.of(2026, 11, 1), app.recurring().get(rule.id()).nextDueDate().orElseThrow());
        assertTrue(app.recurring().postDue().isEmpty(), "second run posts nothing");

        app.commands().undo();
        assertEquals(0, app.transactions().count());
        assertEquals(0, app.recurring().get(rule.id()).postedCount());
    }

    @Test
    void postNextSkipAndUpcoming() {
        RecurringRule rule = rent(LocalDate.of(2026, 11, 1));
        assertEquals(1, app.recurring().upcoming(30).size());
        app.recurring().skipNext(rule.id());
        assertEquals(LocalDate.of(2026, 12, 1), app.recurring().get(rule.id()).nextDueDate().orElseThrow());
        Transaction early = app.recurring().postNext(rule.id());
        assertEquals(LocalDate.of(2026, 12, 1), early.date());
        app.recurring().setActive(rule.id(), false);
        assertTrue(app.recurring().upcoming(90).isEmpty());
    }

    @Test
    void savingsGoalsDepositAndWithdraw() {
        SavingsGoal goal = app.goals().save(SavingsGoal.create("旅行", Money.of(10000), null, 0));
        app.goals().deposit(goal.id(), Money.of(4000));
        app.goals().withdraw(goal.id(), Money.of(1000));
        assertEquals(Money.of(3000), app.goals().get(goal.id()).saved());
        assertThrows(ValidationException.class, () -> app.goals().withdraw(goal.id(), Money.of(5000)));
        app.commands().undo();
        assertEquals(Money.of(4000), app.goals().get(goal.id()).saved());
        assertEquals(Money.of(4000), app.goals().totalSaved());
    }

    @Test
    void treesCountOnlyManualRecords() {
        for (int i = 0; i < 19; i++) {
            Fixtures.expense(app, "餐飲", 10, TODAY.minusDays(i % 3), "");
        }
        rent(LocalDate.of(2026, 10, 1));
        app.recurring().postDue();
        assertEquals(TreeStage.YOUNG_TREE, app.trees().currentTree().stage());
        Fixtures.expense(app, "餐飲", 10, TODAY, "");
        assertEquals(TreeStage.FULL_TREE, app.trees().currentTree().stage());

        app.settings().setTreeGoal(30);
        assertEquals(TreeStage.SAPLING, app.trees().currentTree().stage());
        assertThrows(ValidationException.class, () -> app.settings().setTreeGoal(1));
    }

    @Test
    void forestAndStreaks() {
        Fixtures.expense(app, "餐飲", 10, LocalDate.of(2026, 8, 20), "");
        Fixtures.expense(app, "餐飲", 10, TODAY.minusDays(1), "");
        Fixtures.expense(app, "餐飲", 10, TODAY.minusDays(2), "");
        Forest forest = app.trees().forest(12);
        assertEquals(List.of(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)),
                forest.trees().stream().map(t -> t.month()).toList());
        assertEquals(TreeStage.WITHERED, forest.trees().get(1).stage());
        assertEquals(2, forest.currentStreak(), "streak survives until the day ends");
        assertEquals(2, app.trees().forest(1).longestStreak());
        assertEquals(1, app.trees().forest(1).trees().size());

        Set<LocalDate> days = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(3));
        assertEquals(2, TreeService.currentStreak(days, TODAY));
        assertEquals(3, TreeService.longestStreak(new TreeSet<>(List.of(LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 9)))));
    }
}
