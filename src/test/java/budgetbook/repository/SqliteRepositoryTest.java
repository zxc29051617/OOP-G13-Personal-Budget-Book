package budgetbook.repository;

import budgetbook.exception.EntityNotFoundException;
import budgetbook.exception.StorageException;
import budgetbook.model.Budget;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.DateRange;
import budgetbook.model.Frequency;
import budgetbook.model.Money;
import budgetbook.model.RecurringRule;
import budgetbook.model.SavingsGoal;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.repository.jdbc.SchemaMigrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteRepositoryTest {
    private Storage storage;
    private Wallet cash;
    private Wallet bank;
    private Category food;
    private Category salary;

    @BeforeEach
    void setUp() {
        storage = StorageFactory.open(StorageConfig.inMemory());
        cash = storage.wallets().save(Wallet.create("現金", WalletType.CASH, Money.of(1000), 0));
        bank = storage.wallets().save(Wallet.create("銀行", WalletType.BANK, Money.of(5000), 0));
        food = storage.categories().save(Category.create("餐飲", CategoryKind.EXPENSE, 0xE76F51, ""));
        salary = storage.categories().save(Category.create("薪資", CategoryKind.INCOME, 0x4C9A62, ""));
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    private Transaction expense(LocalDate date, long amount, String note) {
        return storage.transactions().save(Transaction.builder().expense().date(date).time(LocalTime.of(12, 0))
                .amount(amount).categoryId(food.id()).walletId(cash.id()).note(note).build());
    }

    @Test
    void schemaIsMigratedToLatestVersion() {
        assertEquals(SchemaMigrator.latestVersion(), storage.info().schemaVersion());
        assertEquals("SQLite", storage.info().backend());
    }

    @Test
    void fileDatabaseKeepsDataBetweenSessions(@TempDir Path dir) {
        Path file = dir.resolve("data/budget.db");
        try (Storage first = StorageFactory.open(StorageConfig.sqlite(file))) {
            first.wallets().save(Wallet.create("現金", WalletType.CASH, Money.of(10), 0));
            first.settings().put("ui.theme", "dark");
        }
        try (Storage second = StorageFactory.open(StorageConfig.sqlite(file))) {
            assertEquals(1, second.wallets().count());
            assertEquals("dark", second.settings().get("ui.theme").orElseThrow());
            assertEquals(SchemaMigrator.latestVersion(), second.info().schemaVersion());
        }
    }

    @Test
    void crudRoundTripPreservesValues() {
        Transaction saved = storage.transactions().save(Transaction.builder().income().date(LocalDate.of(2026, 10, 5))
                .time(LocalTime.of(9, 30)).amount(Money.parse("42000.50")).categoryId(salary.id()).walletId(bank.id())
                .note("十月薪資").recurringRuleId(3L).build());
        assertTrue(saved.id() > 0);
        assertEquals(saved, storage.transactions().getById(saved.id()));

        Transaction changed = saved.toBuilder().amount(43000).note("調薪").build();
        storage.transactions().save(changed);
        assertEquals(changed, storage.transactions().getById(saved.id()));

        storage.transactions().deleteById(saved.id());
        assertTrue(storage.transactions().findById(saved.id()).isEmpty());
        assertThrows(EntityNotFoundException.class, () -> storage.transactions().deleteById(saved.id()));

        storage.transactions().restore(changed);
        assertEquals(changed, storage.transactions().getById(saved.id()));
    }

    @Test
    void updatingMissingEntityFails() {
        Wallet ghost = new Wallet(999, "幽靈", WalletType.CASH, Money.ZERO, 0, false);
        assertThrows(EntityNotFoundException.class, () -> storage.wallets().save(ghost));
    }

    @Test
    void foreignKeysProtectReferencedRows() {
        expense(LocalDate.of(2026, 10, 1), 100, "");
        assertThrows(StorageException.class, () -> storage.wallets().deleteById(cash.id()));
        assertThrows(StorageException.class, () -> storage.categories().deleteById(food.id()));
    }

    @Test
    void uniqueNamesAreEnforcedByTheDatabase() {
        assertThrows(StorageException.class,
                () -> storage.wallets().save(Wallet.create("現金", WalletType.CASH, Money.ZERO, 0)));
        // 同名但不同類型的分類可以共存
        storage.categories().save(Category.create("餐飲", CategoryKind.INCOME, 0, ""));
        assertEquals(1, storage.categories().findByKind(CategoryKind.INCOME).stream()
                .filter(c -> c.name().equals("餐飲")).count());
    }

    @Test
    void queryFiltersByRangeTypeCategoryWalletAndText() {
        expense(LocalDate.of(2026, 9, 30), 80, "早餐");
        Transaction lunch = expense(LocalDate.of(2026, 10, 1), 120, "午餐 100% 牛肉");
        expense(LocalDate.of(2026, 10, 2), 60, "a_b");
        storage.transactions().save(Transaction.builder().transfer(bank.id(), cash.id()).date(LocalDate.of(2026, 10, 3))
                .amount(500).note("提款").build());

        TransactionQuery october = TransactionQuery.all().withRange(DateRange.of(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)));
        assertEquals(3, storage.transactions().find(october).size());
        assertEquals(2, storage.transactions().find(october.withType(TransactionType.EXPENSE)).size());
        assertEquals(1, storage.transactions().find(TransactionQuery.all().withWallet(bank.id())).size());
        assertEquals(4, storage.transactions().find(TransactionQuery.all().withWallet(cash.id())).size());
        assertEquals(3, storage.transactions().find(TransactionQuery.all().withCategory(food.id())).size());
        assertEquals(List.of(lunch), storage.transactions().find(TransactionQuery.all().withText("100%")));
        assertEquals(1, storage.transactions().find(TransactionQuery.all().withText("_")).size());
        // 文字也會比對分類與錢包名稱
        assertEquals(3, storage.transactions().find(TransactionQuery.all().withText("餐飲")).size());
        assertEquals(1, storage.transactions().find(TransactionQuery.all().withText("銀行")).size());
        // 新到舊排序
        List<Transaction> all = storage.transactions().find(TransactionQuery.all());
        assertEquals(LocalDate.of(2026, 10, 3), all.get(0).date());
        assertEquals(LocalDate.of(2026, 9, 30), all.get(all.size() - 1).date());
    }

    @Test
    void netChangeByWalletSumsInDatabase() {
        expense(LocalDate.of(2026, 10, 1), 120, "");
        storage.transactions().save(Transaction.builder().income().date(LocalDate.of(2026, 10, 5)).amount(1000)
                .categoryId(salary.id()).walletId(bank.id()).build());
        storage.transactions().save(Transaction.builder().transfer(bank.id(), cash.id()).date(LocalDate.of(2026, 10, 6))
                .amount(300).build());
        Map<Long, Money> all = storage.transactions().netChangeByWallet(null);
        assertEquals(Money.of(180), all.get(cash.id()));
        assertEquals(Money.of(700), all.get(bank.id()));
        Map<Long, Money> early = storage.transactions().netChangeByWallet(LocalDate.of(2026, 10, 5));
        assertEquals(Money.of(-120), early.get(cash.id()));
        assertEquals(Money.of(1000), early.get(bank.id()));
    }

    @Test
    void helperQueries() {
        expense(LocalDate.of(2026, 10, 3), 50, "咖啡");
        expense(LocalDate.of(2026, 9, 3), 50, "咖啡");
        expense(LocalDate.of(2026, 10, 4), 90, "午餐");
        assertEquals(LocalDate.of(2026, 9, 3), storage.transactions().earliestDate().orElseThrow());
        assertEquals(List.of("午餐", "咖啡"), storage.transactions().recentNotes(5));
        assertEquals(3, storage.transactions().countByCategory(food.id()));
        assertEquals(3, storage.transactions().countByWallet(cash.id()));
        assertEquals(2, storage.transactions().findRecent(2).size());
    }

    @Test
    void budgetsGoalsRulesAndSettings() {
        Budget overall = storage.budgets().save(Budget.overall(Money.of(20000)));
        storage.budgets().save(Budget.forCategory(food.id(), Money.of(6000)));
        assertEquals(overall, storage.budgets().findByCategoryId(null).orElseThrow());
        assertEquals(Money.of(6000), storage.budgets().findByCategoryId(food.id()).orElseThrow().monthlyLimit());

        SavingsGoal goal = storage.savingsGoals().save(new SavingsGoal(0, "旅行", Money.of(30000), Money.of(100),
                LocalDate.of(2027, 1, 31), 0x123456));
        assertEquals(goal, storage.savingsGoals().getById(goal.id()));

        RecurringRule rule = storage.recurringRules().save(new RecurringRule(0, "房租", TransactionType.EXPENSE,
                Money.of(9500), food.id(), cash.id(), null, "", Frequency.MONTHLY, 1, LocalDate.of(2026, 1, 1),
                null, 3, true));
        assertEquals(rule, storage.recurringRules().getById(rule.id()));
        assertEquals(1, storage.recurringRules().countByCategory(food.id()));
        assertEquals(1, storage.recurringRules().countByWallet(cash.id()));

        storage.settings().put("a", "1");
        storage.settings().put("a", "2");
        assertEquals(Map.of("a", "2"), storage.settings().all());
        storage.settings().remove("a");
        assertFalse(storage.settings().get("a").isPresent());
    }

    @Test
    void transactionsRollBackOnFailure() {
        assertThrows(IllegalStateException.class, () -> storage.inTransaction(() -> {
            expense(LocalDate.of(2026, 10, 1), 100, "");
            throw new IllegalStateException("boom");
        }));
        assertEquals(0, storage.transactions().count());
    }

    @Test
    void clearAllDataKeepsSettings() {
        expense(LocalDate.of(2026, 10, 1), 100, "");
        storage.settings().put("ui.theme", "dark");
        storage.clearAllData();
        assertEquals(0, storage.transactions().count());
        assertEquals(0, storage.wallets().count());
        assertEquals("dark", storage.settings().get("ui.theme").orElseThrow());
    }
}
