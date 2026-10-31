package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 儲蓄目標（類似 Firefly III 的 piggy bank），例如「日本旅行 60,000 元」。
 *
 * @param deadline 目標日期，可為 null
 */
public record SavingsGoal(long id, String name, Money target, Money saved, LocalDate deadline, int color)
        implements Entity {
    public SavingsGoal {
        name = Checks.requireText(name, "目標名稱", 30);
        Checks.requirePositive(target, "目標金額");
        Checks.requireNotNegative(saved, "已存金額");
        color = color & 0xFFFFFF;
    }

    public static SavingsGoal create(String name, Money target, LocalDate deadline, int color) {
        return new SavingsGoal(0, name, target, Money.ZERO, deadline, color);
    }

    public double progress() {
        return Math.min(1.0, saved.ratioOf(target));
    }

    public int percent() {
        return (int) Math.floor(progress() * 100);
    }

    public Money remaining() {
        return Money.max(Money.ZERO, target.minus(saved));
    }

    public boolean isReached() {
        return !saved.isLessThan(target);
    }

    public SavingsGoal deposit(Money amount) {
        Checks.requirePositive(amount, "存入金額");
        return withSaved(saved.plus(amount));
    }

    public SavingsGoal withdraw(Money amount) {
        Checks.requirePositive(amount, "取出金額");
        if (amount.isGreaterThan(saved)) {
            throw new ValidationException("取出金額", "取出金額不能超過已存的 " + saved.format() + "。");
        }
        return withSaved(saved.minus(amount));
    }

    /** 距離目標日期還有幾個月（含本月）；沒有目標日期或已過期時為空。 */
    public Optional<Long> monthsLeft(LocalDate today) {
        if (deadline == null || deadline.isBefore(today)) {
            return Optional.empty();
        }
        return Optional.of(ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)) + 1);
    }

    /** 為了在目標日期前達成，每個月平均還要存多少。 */
    public Optional<Money> monthlyNeeded(LocalDate today) {
        if (isReached()) {
            return Optional.of(Money.ZERO);
        }
        return monthsLeft(today).map(months -> remaining().dividedBy(months));
    }

    public SavingsGoal withId(long newId) {
        return new SavingsGoal(newId, name, target, saved, deadline, color);
    }

    public SavingsGoal withSaved(Money amount) {
        return new SavingsGoal(id, name, target, amount, deadline, color);
    }
}
