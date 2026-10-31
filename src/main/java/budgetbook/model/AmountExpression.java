package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金額欄位的簡單四則運算，例如輸入 {@code 120+45} 或 {@code 3*85} 直接算出金額。
 *
 * <p>以遞迴下降法解析：expression = term (('+'|'-') term)*，term = factor (('*'|'/') factor)*，
 * factor = number | '(' expression ')'。</p>
 */
public final class AmountExpression {
    private final String text;
    private int pos;

    private AmountExpression(String text) {
        this.text = text;
    }

    public static Money evaluate(String input) {
        String cleaned = Money.clean(input).replace('×', '*').replace('÷', '/');
        if (cleaned.isEmpty()) {
            throw new ValidationException("金額", "請輸入金額。");
        }
        AmountExpression parser = new AmountExpression(cleaned);
        try {
            BigDecimal result = parser.expression();
            if (parser.pos != cleaned.length()) {
                throw parser.error();
            }
            return Money.of(result);
        } catch (ArithmeticException e) {
            throw new ValidationException("金額", "算式無法計算：" + input.strip());
        }
    }

    private BigDecimal expression() {
        BigDecimal result = term();
        while (pos < text.length()) {
            char op = text.charAt(pos);
            if (op == '+') {
                pos++;
                result = result.add(term());
            } else if (op == '-') {
                pos++;
                result = result.subtract(term());
            } else {
                break;
            }
        }
        return result;
    }

    private BigDecimal term() {
        BigDecimal result = factor();
        while (pos < text.length()) {
            char op = text.charAt(pos);
            if (op == '*') {
                pos++;
                result = result.multiply(factor());
            } else if (op == '/') {
                pos++;
                result = result.divide(factor(), 10, RoundingMode.HALF_UP);
            } else {
                break;
            }
        }
        return result;
    }

    private BigDecimal factor() {
        if (pos < text.length() && text.charAt(pos) == '(') {
            pos++;
            BigDecimal inner = expression();
            if (pos >= text.length() || text.charAt(pos) != ')') {
                throw error();
            }
            pos++;
            return inner;
        }
        if (pos < text.length() && text.charAt(pos) == '-') {
            pos++;
            return factor().negate();
        }
        int start = pos;
        while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
            pos++;
        }
        if (start == pos) {
            throw error();
        }
        try {
            return new BigDecimal(text.substring(start, pos));
        } catch (NumberFormatException e) {
            throw error();
        }
    }

    private ValidationException error() {
        return new ValidationException("金額", "金額格式不正確：" + text);
    }
}
