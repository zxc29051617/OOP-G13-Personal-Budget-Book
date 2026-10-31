package budgetbook.service.assistant;

import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.DateRange;
import budgetbook.model.SavingsGoal;
import budgetbook.model.Wallet;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** 解析後的問題：正規化文字、時間範圍，以及問句中提到的分類、錢包或儲蓄目標。 */
record Question(String text, PeriodParser.Parsed period, Category category, Wallet wallet, SavingsGoal goal) {

    static Question parse(String raw, AssistantServices services) {
        String text = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFKC)
                .strip().toLowerCase(Locale.ROOT);
        LocalDate today = services.today();
        PeriodParser.Parsed period = PeriodParser.parse(text, today,
                () -> services.transactions().earliestDate().orElse(today)).orElse(null);
        return new Question(text, period, mentionedCategory(text, services.categories().all()),
                mentioned(text, services.wallets().all(), Wallet::name),
                mentioned(text, services.goals().all(), SavingsGoal::name));
    }

    boolean isBlank() {
        return text.isEmpty();
    }

    boolean has(String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    DateRange rangeOr(DateRange fallback) {
        return period == null ? fallback : period.range();
    }

    String labelOr(String fallback) {
        return period == null ? fallback : period.label();
    }

    private static <T> T mentioned(String text, List<T> items, Function<T, String> name) {
        return items.stream()
                .filter(item -> text.contains(name.apply(item).toLowerCase(Locale.ROOT)))
                .max(Comparator.comparingInt(item -> name.apply(item).length()))
                .orElse(null);
    }

    /** 「其他」這種常見詞只有在後面接著支出、收入、分類時才當成分類。 */
    private static Category mentionedCategory(String text, List<Category> categories) {
        boolean incomeContext = text.contains("收入") || text.contains("賺");
        Optional<Category> best = categories.stream()
                .filter(c -> text.contains(c.name().toLowerCase(Locale.ROOT)))
                .filter(c -> !c.name().equals("其他") || text.matches(".*其他(支出|收入|分類|類).*"))
                .max(Comparator.<Category>comparingInt(c -> c.name().length())
                        .thenComparing(c -> (c.kind() == CategoryKind.INCOME) == incomeContext));
        return best.orElse(null);
    }
}
