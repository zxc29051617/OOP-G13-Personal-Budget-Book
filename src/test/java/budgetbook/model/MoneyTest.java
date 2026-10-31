package budgetbook.model;

import budgetbook.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {
    @Test
    void decimalArithmeticIsExact() {
        Money sum = Money.parse("0.1").plus(Money.parse("0.2"));
        assertEquals(Money.parse("0.3"), sum);
        assertEquals("0.3", sum.toPlainString());
    }

    @Test
    void equalityIgnoresScale() {
        assertEquals(Money.of(new BigDecimal("12.5")), Money.of(new BigDecimal("12.50")));
        assertEquals(Money.of(12).hashCode(), Money.parse("12.00").hashCode());
    }

    @Test
    void operationsReturnNewObjects() {
        Money a = Money.of(100);
        Money b = a.plus(Money.ZERO);
        assertEquals(a, b);
        assertNotSame(a, a.minus(Money.ZERO));
        assertEquals(Money.of(100), a);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1,234|1234",
            "NT$ 99.5|99.5",
            "$120|120",
            "１２０元|120",
            "  42  |42"})
    void parsesUserInput(String input, String expected) {
        assertEquals(new BigDecimal(expected).setScale(2), Money.parse(input).toBigDecimal());
    }

    @Test
    void rejectsGarbage() {
        assertThrows(ValidationException.class, () -> Money.parse("abc"));
        assertThrows(ValidationException.class, () -> Money.parse(" "));
    }

    @Test
    void formatsForDisplay() {
        assertEquals("$1,234", Money.of(1234).format());
        assertEquals("$99.50", Money.parse("99.5").format());
        assertEquals("-$300", Money.of(-300).format());
        assertEquals("+$120", Money.of(120).formatSigned());
    }

    @Test
    void centsRoundTrip() {
        Money money = Money.parse("1234.56");
        assertEquals(123456, money.toCents());
        assertEquals(money, Money.ofCents(money.toCents()));
    }

    @Test
    void ratioAndDivision() {
        assertEquals(0.25, Money.of(25).ratioOf(Money.of(100)), 1e-9);
        assertEquals(0.0, Money.of(25).ratioOf(Money.ZERO));
        assertEquals(Money.parse("33.33"), Money.of(100).dividedBy(3));
        assertEquals(Money.of(5), Money.parse("4.5").roundToDollar());
    }

    @Test
    void sumAndComparison() {
        Money total = Money.sum(Stream.of(Money.of(1), Money.of(2), Money.of(3)));
        assertEquals(Money.of(6), total);
        assertEquals(Money.of(6), Stream.of(Money.of(1), Money.of(5)).collect(Money.summing()));
        assertTrue(Money.of(5).isGreaterThan(Money.of(3)));
        assertFalse(Money.of(-1).isPositive());
        assertEquals(Money.of(3), Money.min(Money.of(3), Money.of(5)));
    }
}
