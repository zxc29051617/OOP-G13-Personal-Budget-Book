package budgetbook.service.assistant;

import budgetbook.model.DateRange;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 從中文問句找出時間範圍，例如「上個月」「2026年3月」「最近7天」「今年」「三月」。
 */
final class PeriodParser {
    record Parsed(DateRange range, String label) {
    }

    private record Keyword(List<String> words, String key) {
    }

    private static final Pattern YEAR_MONTH = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月");
    private static final Pattern LAST_DAYS = Pattern.compile("(?:最近|近|過去|前)\\s*(\\d{1,3})\\s*(?:天|日)");
    private static final Pattern LAST_MONTHS = Pattern.compile("(?:最近|近|過去|前)\\s*(\\d{1,2})\\s*個?月");
    private static final Pattern YEAR = Pattern.compile("(\\d{4})\\s*年");
    private static final Pattern MONTH = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*月");
    private static final Pattern CHINESE_NUMBER = Pattern.compile("[零一二兩三四五六七八九十]+");

    /** 依序比對，較長、較特定的詞放前面（「上上個月」要在「上個月」之前）。 */
    private static final List<Keyword> KEYWORDS = List.of(
            new Keyword(List.of("今天", "今日"), "today"),
            new Keyword(List.of("昨天", "昨日"), "yesterday"),
            new Keyword(List.of("上週", "上周", "上禮拜", "上星期"), "lastWeek"),
            new Keyword(List.of("本週", "本周", "這週", "這周", "這禮拜", "這星期"), "thisWeek"),
            new Keyword(List.of("上上個月"), "twoMonthsAgo"),
            new Keyword(List.of("上個月", "上月"), "lastMonth"),
            new Keyword(List.of("本月", "這個月", "這月", "當月"), "thisMonth"),
            new Keyword(List.of("去年"), "lastYear"),
            new Keyword(List.of("今年", "本年", "這一年"), "thisYear"),
            new Keyword(List.of("全部", "總共", "所有", "至今", "歷史", "一共", "累計"), "all"));

    private PeriodParser() {
    }

    static Optional<Parsed> parse(String question, LocalDate today, Supplier<LocalDate> earliest) {
        String text = toArabicNumerals(question);
        Matcher m = YEAR_MONTH.matcher(text);
        if (m.find() && validMonth(m.group(2))) {
            YearMonth month = YearMonth.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            return Optional.of(new Parsed(DateRange.ofMonth(month), month.getYear() + " 年 " + month.getMonthValue() + " 月"));
        }
        m = LAST_DAYS.matcher(text);
        if (m.find()) {
            int days = Math.max(1, Integer.parseInt(m.group(1)));
            return Optional.of(new Parsed(DateRange.lastDays(today, days), "最近 " + days + " 天"));
        }
        m = LAST_MONTHS.matcher(text);
        if (m.find()) {
            int months = Math.max(1, Integer.parseInt(m.group(1)));
            LocalDate start = YearMonth.from(today).minusMonths(months - 1L).atDay(1);
            return Optional.of(new Parsed(new DateRange(start, today), "最近 " + months + " 個月"));
        }
        m = YEAR.matcher(text);
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            return Optional.of(new Parsed(DateRange.ofYear(year), year + " 年"));
        }
        m = MONTH.matcher(text);
        if (m.find() && validMonth(m.group(1))) {
            int monthValue = Integer.parseInt(m.group(1));
            int year = monthValue > today.getMonthValue() ? today.getYear() - 1 : today.getYear();
            YearMonth month = YearMonth.of(year, monthValue);
            return Optional.of(new Parsed(DateRange.ofMonth(month), year + " 年 " + monthValue + " 月"));
        }
        for (Keyword keyword : KEYWORDS) {
            for (String word : keyword.words()) {
                if (text.contains(word)) {
                    return Optional.of(resolve(keyword.key(), word, today, earliest));
                }
            }
        }
        return Optional.empty();
    }

    private static Parsed resolve(String key, String word, LocalDate today, Supplier<LocalDate> earliest) {
        YearMonth month = YearMonth.from(today);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        return switch (key) {
            case "today" -> new Parsed(DateRange.ofDay(today), "今天");
            case "yesterday" -> new Parsed(DateRange.ofDay(today.minusDays(1)), "昨天");
            case "lastWeek" -> new Parsed(new DateRange(monday.minusWeeks(1), monday.minusDays(1)), "上週");
            case "thisWeek" -> new Parsed(new DateRange(monday, today), "本週");
            case "twoMonthsAgo" -> monthOf(month.minusMonths(2));
            case "lastMonth" -> monthOf(month.minusMonths(1));
            case "thisMonth" -> new Parsed(DateRange.ofMonth(month), "本月");
            case "lastYear" -> new Parsed(DateRange.ofYear(today.getYear() - 1), (today.getYear() - 1) + " 年");
            case "thisYear" -> new Parsed(DateRange.ofYear(today.getYear()), "今年");
            default -> {
                LocalDate first = earliest.get();
                yield new Parsed(new DateRange(first.isAfter(today) ? today : first, today), "目前為止");
            }
        };
    }

    private static Parsed monthOf(YearMonth month) {
        return new Parsed(DateRange.ofMonth(month), month.getYear() + " 年 " + month.getMonthValue() + " 月");
    }

    private static boolean validMonth(String text) {
        int month = Integer.parseInt(text);
        return month >= 1 && month <= 12;
    }

    /** 把「十二」「二十五」「三」等中文數字換成阿拉伯數字，只用於判斷時間。 */
    static String toArabicNumerals(String text) {
        Matcher m = CHINESE_NUMBER.matcher(text);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(result, Integer.toString(chineseToInt(m.group())));
        }
        m.appendTail(result);
        return result.toString();
    }

    private static int chineseToInt(String text) {
        String digits = "零一二三四五六七八九";
        int value = 0;
        int current = 0;
        for (char c : text.replace('兩', '二').toCharArray()) {
            if (c == '十') {
                value += (current == 0 ? 1 : current) * 10;
                current = 0;
            } else {
                current = current * 10 + digits.indexOf(c);
            }
        }
        return value + current;
    }
}
