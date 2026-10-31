package budgetbook.model;

import budgetbook.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 15);

    private static Transaction.Builder lunch() {
        return Transaction.builder().expense().date(DAY).time(LocalTime.of(12, 30, 45)).amount(120)
                .categoryId(1L).walletId(1).note("  午餐  ");
    }

    @Test
    void builderNormalizesFields() {
        Transaction t = lunch().build();
        assertEquals(LocalTime.of(12, 30), t.time());
        assertEquals("午餐", t.note());
        assertEquals(Money.of(-120), t.balanceEffectOn(1));
        assertEquals(Money.ZERO, t.balanceEffectOn(2));
        assertEquals(Money.of(-120), t.netWorthEffect());
    }

    @Test
    void amountMustBePositive() {
        ValidationException e = assertThrows(ValidationException.class, () -> lunch().amount(0).build());
        assertEquals("金額", e.field());
    }

    @Test
    void incomeAndExpenseNeedCategory() {
        assertThrows(ValidationException.class, () -> lunch().categoryId(null).build());
    }

    @Test
    void transferMovesMoneyBetweenWallets() {
        Transaction t = Transaction.builder().transfer(1, 2).date(DAY).amount(500).categoryId(9L).build();
        assertNull(t.categoryId());
        assertEquals(Money.of(-500), t.balanceEffectOn(1));
        assertEquals(Money.of(500), t.balanceEffectOn(2));
        assertEquals(Money.ZERO, t.netWorthEffect());
    }

    @Test
    void transferNeedsTwoDifferentWallets() {
        assertThrows(ValidationException.class,
                () -> Transaction.builder().transfer(1, 1).date(DAY).amount(500).build());
        assertThrows(ValidationException.class,
                () -> Transaction.builder().type(TransactionType.TRANSFER).walletId(1).date(DAY).amount(5).build());
    }

    @Test
    void noteLengthIsLimited() {
        assertThrows(ValidationException.class, () -> lunch().note("字".repeat(201)).build());
    }

    @Test
    void toBuilderCopiesAllFields() {
        Transaction t = lunch().id(7).recurringRuleId(3L).build();
        assertEquals(t, t.toBuilder().build());
        assertEquals(8, t.withId(8).id());
    }

    @Test
    void typeParsingAcceptsChineseAndEnglish() {
        assertEquals(TransactionType.EXPENSE, TransactionType.parse("支出"));
        assertEquals(TransactionType.INCOME, TransactionType.parse("income"));
        assertThrows(ValidationException.class, () -> TransactionType.parse("借款"));
    }
}
