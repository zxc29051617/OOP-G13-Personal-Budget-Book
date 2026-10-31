package budgetbook.service;

import budgetbook.Fixtures;
import budgetbook.exception.EntityInUseException;
import budgetbook.exception.ValidationException;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static budgetbook.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletAndCategoryServiceTest {
    private final AppContext app = Fixtures.app();

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void starterDataMatchesOriginalVersion() {
        assertEquals(3, app.wallets().all().size());
        assertEquals(Money.of(19000), app.wallets().totalBalance());
        assertFalse(app.categories().byKind(CategoryKind.EXPENSE).isEmpty());
        assertFalse(app.demo().ensureStarterData(), "second call does nothing");
    }

    @Test
    void balancesIncludeIncomeExpenseAndTransfers() {
        Wallet cash = Fixtures.wallet(app, "現金");
        Wallet bank = Fixtures.wallet(app, "銀行帳戶");
        Fixtures.expense(app, "餐飲", 120, TODAY, "午餐");
        Fixtures.income(app, "薪資", 30000, TODAY);
        app.transactions().add(Transaction.builder().transfer(bank.id(), cash.id()).date(TODAY).amount(2000).build());

        assertEquals(Money.of(3000 - 120 + 2000), app.wallets().balanceOf(cash.id()));
        assertEquals(Money.of(15000 + 30000 - 2000), app.wallets().balanceOf(bank.id()));
        assertEquals(Money.of(19000 + 30000 - 120), app.wallets().totalBalance());
        assertEquals(Money.of(19000), app.wallets().totalBalanceAsOf(TODAY.minusDays(1)));
    }

    @Test
    void walletNamesAreUniqueAndUsedWalletsCannotBeDeleted() {
        assertThrows(ValidationException.class,
                () -> app.wallets().create("現金", WalletType.CASH, Money.ZERO, 0));
        Wallet cash = Fixtures.wallet(app, "現金");
        Fixtures.expense(app, "餐飲", 50, TODAY, "");
        assertThrows(EntityInUseException.class, () -> app.wallets().delete(cash.id()));

        app.wallets().setArchived(cash.id(), true);
        assertFalse(app.wallets().active().contains(app.wallets().get(cash.id())));
        Wallet unused = app.wallets().create("悠遊卡", WalletType.E_PAYMENT, Money.of(300), 0);
        app.wallets().delete(unused.id());
        assertEquals(3, app.wallets().all().size());
    }

    @Test
    void archivedWalletCannotTakeNewTransactions() {
        Wallet cash = Fixtures.wallet(app, "現金");
        app.wallets().setArchived(cash.id(), true);
        assertThrows(ValidationException.class, () -> Fixtures.expense(app, "餐飲", 50, TODAY, ""));
    }

    @Test
    void deleteCategoryAlsoDeletesItsBudgetAndUndoRestoresBoth() {
        Category hobby = app.categories().create("手作", CategoryKind.EXPENSE, 0x123456, "手");
        app.budgets().setForCategory(hobby.id(), Money.of(800));
        app.categories().delete(hobby.id());
        assertTrue(app.budgets().forCategory(hobby.id()).isEmpty());
        app.commands().undo();
        assertEquals(hobby, app.categories().get(hobby.id()));
        assertEquals(Money.of(800), app.budgets().forCategory(hobby.id()).orElseThrow().monthlyLimit());
    }

    @Test
    void usedCategoryCanBeMergedButNotDeleted() {
        Category food = Fixtures.category(app, CategoryKind.EXPENSE, "餐飲");
        Category other = Fixtures.category(app, CategoryKind.EXPENSE, "其他");
        Fixtures.expense(app, "餐飲", 100, TODAY, "a");
        Fixtures.expense(app, "餐飲", 200, TODAY, "b");
        assertThrows(EntityInUseException.class, () -> app.categories().delete(food.id()));
        assertThrows(ValidationException.class, () -> app.categories().mergeInto(food.id(),
                Fixtures.category(app, CategoryKind.INCOME, "薪資").id()));

        assertEquals(2, app.categories().mergeInto(food.id(), other.id()));
        assertTrue(app.categories().find(food.id()).isEmpty());
        assertEquals(2, app.transactions().search(TransactionQuery.all().withCategory(other.id())).size());
        app.commands().undo();
        assertEquals(2, app.transactions().search(TransactionQuery.all().withCategory(food.id())).size());
    }

    @Test
    void categoryRulesAreValidated() {
        assertThrows(ValidationException.class,
                () -> app.categories().create("餐飲", CategoryKind.EXPENSE, 0, ""));
        Category food = Fixtures.category(app, CategoryKind.EXPENSE, "餐飲");
        Fixtures.expense(app, "餐飲", 100, LocalDate.of(2026, 10, 1), "");
        assertThrows(ValidationException.class, () -> app.categories().update(
                new Category(food.id(), food.name(), CategoryKind.INCOME, food.color(), food.icon())));
        Category renamed = app.categories().update(food.withName("吃飯").withColor(0x00FF00));
        assertEquals("吃飯", app.categories().get(food.id()).name());
        assertEquals(0x00FF00, renamed.color());
    }
}
