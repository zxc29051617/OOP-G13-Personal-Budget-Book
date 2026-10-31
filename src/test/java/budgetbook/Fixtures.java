package budgetbook;

import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Transaction;
import budgetbook.model.Wallet;
import budgetbook.repository.StorageConfig;
import budgetbook.service.AppContext;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** 測試共用：固定的「今天」與記憶體資料庫。 */
public final class Fixtures {
    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    private Fixtures() {
    }

    public static Clock clock() {
        return Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
    }

    /** 空的記憶體資料庫。 */
    public static AppContext emptyApp() {
        return AppContext.open(StorageConfig.inMemory(), clock());
    }

    /** 有預設分類與「現金、銀行帳戶、電子支付」三個錢包。 */
    public static AppContext app() {
        AppContext app = emptyApp();
        app.demo().ensureStarterData();
        app.commands().clearHistory();
        return app;
    }

    public static Wallet wallet(AppContext app, String name) {
        return app.wallets().all().stream().filter(w -> w.name().equals(name)).findFirst().orElseThrow();
    }

    public static Category category(AppContext app, CategoryKind kind, String name) {
        return app.categories().byKind(kind).stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    public static Transaction expense(AppContext app, String category, long amount, LocalDate date, String note) {
        return app.transactions().add(Transaction.builder().expense().date(date).time(LocalTime.of(12, 0))
                .amount(amount).categoryId(category(app, CategoryKind.EXPENSE, category).id())
                .walletId(wallet(app, "現金").id()).note(note).build());
    }

    public static Transaction income(AppContext app, String category, long amount, LocalDate date) {
        return app.transactions().add(Transaction.builder().income().date(date).time(LocalTime.of(9, 0))
                .amount(amount).categoryId(category(app, CategoryKind.INCOME, category).id())
                .walletId(wallet(app, "銀行帳戶").id()).build());
    }
}
