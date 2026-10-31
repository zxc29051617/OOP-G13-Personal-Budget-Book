package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** 日期區間（含頭尾兩天）。 */
public record DateRange(LocalDate start, LocalDate end) {
    public DateRange {
        Checks.requireValue(start, "開始日期");
        Checks.requireValue(end, "結束日期");
        if (end.isBefore(start)) {
            throw new ValidationException("結束日期", "結束日期不能早於開始日期。");
        }
    }

    public static DateRange of(LocalDate start, LocalDate end) {
        return new DateRange(start, end);
    }

    public static DateRange ofMonth(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }

    public static DateRange ofYear(int year) {
        return new DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public static DateRange ofDay(LocalDate day) {
        return new DateRange(day, day);
    }

    /** 包含今天在內的最近 {@code days} 天。 */
    public static DateRange lastDays(LocalDate today, int days) {
        return new DateRange(today.minusDays(Math.max(1, days) - 1L), today);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    public long days() {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    public boolean isFullMonth() {
        return start.getDayOfMonth() == 1 && YearMonth.from(start).atEndOfMonth().equals(end);
    }

    public boolean isFullYear() {
        return start.getDayOfYear() == 1 && start.getYear() == end.getYear()
                && end.equals(Year.of(start.getYear()).atMonth(12).atEndOfMonth());
    }

    public List<YearMonth> months() {
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
            months.add(m);
        }
        return months;
    }

    public List<LocalDate> dates() {
        return start.datesUntil(end.plusDays(1)).toList();
    }

    /** 前一個同樣長度的區間；整月取上個月、整年取去年。 */
    public DateRange previous() {
        if (isFullMonth()) {
            return ofMonth(YearMonth.from(start).minusMonths(1));
        }
        if (isFullYear()) {
            return ofYear(start.getYear() - 1);
        }
        return new DateRange(start.minusDays(days()), start.minusDays(1));
    }

    /** 例如「2026 年 10 月」「2026 年」「2026-10-01 ~ 2026-10-15」。 */
    public String label() {
        if (isFullMonth()) {
            return start.getYear() + " 年 " + start.getMonthValue() + " 月";
        }
        if (isFullYear()) {
            return start.getYear() + " 年";
        }
        return start.equals(end) ? start.toString() : start + " ~ " + end;
    }
}
