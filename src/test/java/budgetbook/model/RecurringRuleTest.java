package budgetbook.model;

import budgetbook.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringRuleTest {
    private static RecurringRule monthly(LocalDate start, LocalDate end, int posted) {
        return new RecurringRule(1, "房租", TransactionType.EXPENSE, Money.of(9500), 1L, 1, null, "",
                Frequency.MONTHLY, 1, start, end, posted, true);
    }

    @Test
    void monthEndDoesNotDrift() {
        RecurringRule rule = monthly(LocalDate.of(2026, 1, 31), null, 0);
        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30)), rule.dueDates(LocalDate.of(2026, 4, 30)));
    }

    @Test
    void postedCountMovesNextDueDate() {
        RecurringRule rule = monthly(LocalDate.of(2026, 1, 5), null, 3);
        assertEquals(LocalDate.of(2026, 4, 5), rule.nextDueDate().orElseThrow());
        assertEquals(List.of(LocalDate.of(2026, 4, 5)), rule.dueDates(LocalDate.of(2026, 4, 30)));
    }

    @Test
    void endDateStopsTheRule() {
        RecurringRule rule = monthly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 3);
        assertTrue(rule.isFinished());
        assertTrue(rule.dueDates(LocalDate.of(2027, 1, 1)).isEmpty());
    }

    @Test
    void inactiveRulesAreNeverDue() {
        RecurringRule rule = monthly(LocalDate.of(2026, 1, 1), null, 0).withActive(false);
        assertTrue(rule.dueDates(LocalDate.of(2026, 12, 31)).isEmpty());
    }

    @Test
    void weeklyIntervalAndDescriptions() {
        RecurringRule rule = new RecurringRule(0, "社團", TransactionType.EXPENSE, Money.of(100), 1L, 1, null, "",
                Frequency.WEEKLY, 2, LocalDate.of(2026, 10, 1), null, 0, true);
        assertEquals(List.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 29)),
                rule.upcomingUntil(LocalDate.of(2026, 11, 1), 10));
        assertEquals("每 2 週", rule.scheduleText());
        assertEquals("每月", Frequency.MONTHLY.describe(1));
    }

    @Test
    void generatedTransactionKeepsRuleReference() {
        Transaction t = monthly(LocalDate.of(2026, 1, 5), null, 0).toTransaction(LocalDate.of(2026, 1, 5));
        assertEquals(1L, t.recurringRuleId());
        assertEquals("房租", t.note());
        assertEquals(RecurringRule.POSTING_TIME, t.time());
    }

    @Test
    void validatesDatesAndInterval() {
        assertThrows(ValidationException.class,
                () -> monthly(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), 0));
        assertThrows(ValidationException.class, () -> new RecurringRule(0, "x", TransactionType.EXPENSE,
                Money.of(1), 1L, 1, null, "", Frequency.DAILY, 0, LocalDate.of(2026, 1, 1), null, 0, true));
    }
}
