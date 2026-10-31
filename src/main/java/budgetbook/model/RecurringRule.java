package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 定期交易規則，例如每月 5 日的薪資、每月 1 日的房租。
 *
 * <p>以「開始日 + 已入帳次數」推算下一次日期，不另外存下一次日期，避免兩個欄位不一致。</p>
 *
 * @param endDate     最後一次可以發生的日期，null 代表沒有結束
 * @param postedCount 已經入帳的次數
 */
public record RecurringRule(long id, String name, TransactionType type, Money amount, Long categoryId,
                            long walletId, Long toWalletId, String note, Frequency frequency, int interval,
                            LocalDate startDate, LocalDate endDate, int postedCount, boolean active)
        implements Entity {
    /** 一次最多補入帳的次數，避免開始日設錯（例如 1900 年）時產生大量交易。 */
    public static final int MAX_CATCH_UP = 400;
    public static final LocalTime POSTING_TIME = LocalTime.of(8, 0);

    public RecurringRule {
        name = Checks.requireText(name, "名稱", 30);
        Checks.requireValue(type, "交易類型");
        Checks.requirePositive(amount, "金額");
        if (type == TransactionType.TRANSFER) {
            categoryId = null;
        } else {
            toWalletId = null;
        }
        Transaction.validateParties(type, categoryId, walletId, toWalletId);
        note = Checks.optionalText(note, "備註", Transaction.MAX_NOTE_LENGTH);
        Checks.requireValue(frequency, "週期");
        Checks.requireRange(interval, 1, 99, "間隔");
        Checks.requireValue(startDate, "開始日期");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("結束日期", "結束日期不能早於開始日期。");
        }
        if (postedCount < 0) {
            throw new ValidationException("已入帳次數", "已入帳次數不能是負數。");
        }
    }

    public Optional<LocalDate> nextDueDate() {
        LocalDate next = frequency.nth(startDate, interval, postedCount);
        if (endDate != null && next.isAfter(endDate)) {
            return Optional.empty();
        }
        return Optional.of(next);
    }

    public boolean isFinished() {
        return nextDueDate().isEmpty();
    }

    /** 到 {@code today}（含）為止應該入帳但還沒入帳的日期。 */
    public List<LocalDate> dueDates(LocalDate today) {
        if (!active) {
            return List.of();
        }
        return occurrences(postedCount, today, MAX_CATCH_UP);
    }

    /** 從下一次開始，到 {@code until}（含）為止的預定日期，用於「即將到來」清單。 */
    public List<LocalDate> upcomingUntil(LocalDate until, int limit) {
        if (!active) {
            return List.of();
        }
        return occurrences(postedCount, until, limit);
    }

    private List<LocalDate> occurrences(int fromIndex, LocalDate until, int limit) {
        List<LocalDate> dates = new ArrayList<>();
        for (int n = fromIndex; dates.size() < limit; n++) {
            LocalDate date = frequency.nth(startDate, interval, n);
            if (date.isAfter(until) || (endDate != null && date.isAfter(endDate))) {
                break;
            }
            dates.add(date);
        }
        return dates;
    }

    /** 依這條規則產生一筆交易。 */
    public Transaction toTransaction(LocalDate date) {
        return Transaction.builder()
                .type(type)
                .date(date)
                .time(POSTING_TIME)
                .amount(amount)
                .categoryId(categoryId)
                .walletId(walletId)
                .toWalletId(toWalletId)
                .note(note.isBlank() ? name : note)
                .recurringRuleId(id == 0 ? null : id)
                .build();
    }

    public String scheduleText() {
        return frequency.describe(interval);
    }

    public RecurringRule withId(long newId) {
        return new RecurringRule(newId, name, type, amount, categoryId, walletId, toWalletId, note, frequency,
                interval, startDate, endDate, postedCount, active);
    }

    public RecurringRule withPostedCount(int count) {
        return new RecurringRule(id, name, type, amount, categoryId, walletId, toWalletId, note, frequency,
                interval, startDate, endDate, count, active);
    }

    public RecurringRule withActive(boolean value) {
        return new RecurringRule(id, name, type, amount, categoryId, walletId, toWalletId, note, frequency,
                interval, startDate, endDate, postedCount, value);
    }
}
