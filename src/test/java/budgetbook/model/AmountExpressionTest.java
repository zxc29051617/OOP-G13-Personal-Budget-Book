package budgetbook.model;

import budgetbook.exception.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmountExpressionTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "120|120",
            "120+45|165",
            "100-35.5|64.5",
            "3*85|255",
            "3×85|255",
            "(80+40)/2|60",
            "1,200 + 300|1500",
            "１００＋２０|120",
            "-5+10|5"})
    void evaluates(String input, String expected) {
        assertEquals(Money.parse(expected), AmountExpression.evaluate(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12+", "(3", "1/0", "abc", "1..2"})
    void rejectsInvalidInput(String input) {
        assertThrows(ValidationException.class, () -> AmountExpression.evaluate(input));
    }
}
