package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * 金額值物件（Value Object）。
 *
 * <p>以 {@link BigDecimal} 保存、固定兩位小數，避免 {@code double} 的進位誤差；
 * 物件不可變，所有運算都回傳新的 {@code Money}。資料庫中以「分」為單位的整數保存。</p>
 */
public final class Money implements Comparable<Money> {
    public static final int SCALE = 2;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal value;

    private Money(BigDecimal value) {
        this.value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) {
        return new Money(Objects.requireNonNull(value, "value"));
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money ofCents(long cents) {
        return new Money(BigDecimal.valueOf(cents, SCALE));
    }

    /**
     * 解析使用者輸入的金額，可接受千分位逗號、全形數字與 {@code $}、{@code NT$}、{@code 元}。
     *
     * @throws ValidationException 格式不正確時
     */
    public static Money parse(String text) {
        String cleaned = clean(text);
        if (cleaned.isEmpty()) {
            throw new ValidationException("金額", "請輸入金額。");
        }
        try {
            return new Money(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            throw new ValidationException("金額", "金額格式不正確：" + text.strip());
        }
    }

    static String clean(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        return normalized.replace("NT$", "")
                .replace("$", "")
                .replace("元", "")
                .replace(",", "")
                .replaceAll("\\s+", "");
    }

    public static Money sum(Stream<Money> amounts) {
        return amounts.reduce(ZERO, Money::plus);
    }

    public static Collector<Money, ?, Money> summing() {
        return Collector.of(() -> new BigDecimal[]{BigDecimal.ZERO},
                (acc, money) -> acc[0] = acc[0].add(money.value),
                (left, right) -> {
                    left[0] = left[0].add(right[0]);
                    return left;
                },
                acc -> new Money(acc[0]));
    }

    public static Money max(Money a, Money b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static Money min(Money a, Money b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public Money plus(Money other) {
        return new Money(value.add(other.value));
    }

    public Money minus(Money other) {
        return new Money(value.subtract(other.value));
    }

    public Money times(long factor) {
        return new Money(value.multiply(BigDecimal.valueOf(factor)));
    }

    public Money times(BigDecimal factor) {
        return new Money(value.multiply(factor));
    }

    /** 除以整數，四捨五入到兩位小數。 */
    public Money dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("divide by zero");
        }
        return new Money(value.divide(BigDecimal.valueOf(divisor), SCALE, RoundingMode.HALF_UP));
    }

    /** 四捨五入到整數元，用於預估值等不需要角分的顯示。 */
    public Money roundToDollar() {
        return new Money(value.setScale(0, RoundingMode.HALF_UP));
    }

    /** 無條件進位到整數元，例如每月需要存的錢。 */
    public Money ceilToDollar() {
        return new Money(value.setScale(0, RoundingMode.CEILING));
    }

    public Money negate() {
        return new Money(value.negate());
    }

    public Money abs() {
        return new Money(value.abs());
    }

    /** 這個金額占 {@code total} 的比例；{@code total} 為 0 時回傳 0。 */
    public double ratioOf(Money total) {
        if (total.isZero()) {
            return 0;
        }
        return value.divide(total.value, 6, RoundingMode.HALF_UP).doubleValue();
    }

    public int signum() {
        return value.signum();
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public BigDecimal toBigDecimal() {
        return value;
    }

    /** 給圖表使用；金額計算一律使用 {@link BigDecimal}。 */
    public double toDouble() {
        return value.doubleValue();
    }

    public long toCents() {
        return value.unscaledValue().longValueExact();
    }

    /** 不含貨幣符號與千分位的數字字串，例如 {@code 1234} 或 {@code 99.5}，用於 CSV 與輸入框。 */
    public String toPlainString() {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }

    /** 顯示用格式，例如 {@code $1,234}、{@code $99.50}、{@code -$300}。 */
    public String format() {
        BigDecimal abs = value.abs();
        boolean whole = abs.stripTrailingZeros().scale() <= 0;
        DecimalFormat format = new DecimalFormat(whole ? "#,##0" : "#,##0.00");
        return (isNegative() ? "-$" : "$") + format.format(abs);
    }

    /** 一律帶正負號，例如 {@code +$120}、{@code -$85}。 */
    public String formatSigned() {
        return (isNegative() ? "-" : "+") + abs().format();
    }

    @Override
    public int compareTo(Money other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return format();
    }
}
