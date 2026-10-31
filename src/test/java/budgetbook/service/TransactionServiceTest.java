package budgetbook.service;

import budgetbook.Fixtures;
import budgetbook.exception.ValidationException;
import budgetbook.model.CategoryKind;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static budgetbook.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionServiceTest {
    private final AppContext app = Fixtures.app();

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void categoryKindMustMatchTransactionType() {
        long salary = Fixtures.category(app, CategoryKind.INCOME, "薪資").id();
        Transaction wrong = Transaction.builder().expense().date(TODAY).amount(100).categoryId(salary)
                .walletId(Fixtures.wallet(app, "現金").id()).build();
        ValidationException e = assertThrows(ValidationException.class, () -> app.transactions().add(wrong));
        assertTrue(e.getMessage().contains("薪資"));
        assertEquals(0, app.transactions().count());
    }

    @Test
    void addUpdateDeleteAreUndoable() {
        Transaction lunch = Fixtures.expense(app, "餐飲", 120, TODAY, "午餐");
        assertEquals("新增支出 餐飲 $120", app.commands().undoDescription().orElseThrow());
        app.transactions().update(lunch.toBuilder().amount(150).build());
        app.transactions().delete(lunch.id());
        assertEquals(0, app.transactions().count());
        app.commands().undo();
        assertEquals(150, app.transactions().get(lunch.id()).amount().toBigDecimal().intValue());
        app.commands().undo();
        assertEquals(lunch, app.transactions().get(lunch.id()));
    }

    @Test
    void deleteAllIsOneStep() {
        List<Long> ids = List.of(Fixtures.expense(app, "餐飲", 1, TODAY, "").id(),
                Fixtures.expense(app, "交通", 2, TODAY, "").id(), Fixtures.expense(app, "購物", 3, TODAY, "").id());
        app.transactions().deleteAll(ids);
        assertEquals(0, app.transactions().count());
        app.commands().undo();
        assertEquals(3, app.transactions().count());
    }

    @Test
    void duplicateCopiesToToday() {
        Transaction old = Fixtures.expense(app, "餐飲", 85, TODAY.minusDays(3), "早餐");
        Transaction copy = app.transactions().duplicateToToday(old.id());
        assertEquals(TODAY, copy.date());
        assertEquals(LocalTime.NOON, copy.time());
        assertEquals("早餐", copy.note());
        assertNull(copy.recurringRuleId());
    }

    @Test
    void suggestsCategoryFromHistory() {
        Fixtures.expense(app, "餐飲", 60, TODAY, "星巴克");
        Fixtures.expense(app, "餐飲", 60, TODAY, "星巴克");
        Fixtures.expense(app, "娛樂", 60, TODAY, "星巴克 生日禮物");
        Fixtures.expense(app, "交通", 30, TODAY, "捷運");
        long food = Fixtures.category(app, CategoryKind.EXPENSE, "餐飲").id();
        long transport = Fixtures.category(app, CategoryKind.EXPENSE, "交通").id();
        assertEquals(food, app.transactions().suggestCategory("星巴克", TransactionType.EXPENSE).orElseThrow());
        assertEquals(transport, app.transactions().suggestCategory("捷", TransactionType.EXPENSE).orElseThrow());
        assertTrue(app.transactions().suggestCategory("星巴克", TransactionType.INCOME).isEmpty());
        assertTrue(app.transactions().suggestCategory(" ", TransactionType.EXPENSE).isEmpty());
        assertEquals(List.of("捷運", "星巴克 生日禮物", "星巴克"), app.transactions().noteSuggestions(5));
    }

    @Test
    void searchCombinesFilters() {
        Fixtures.expense(app, "餐飲", 100, TODAY, "拉麵");
        Fixtures.expense(app, "餐飲", 100, TODAY.minusMonths(1), "拉麵");
        Fixtures.income(app, "薪資", 1000, TODAY);
        assertEquals(1, app.transactions().search(TransactionQuery.all().withText("拉麵")
                .withRange(budgetbook.model.DateRange.ofMonth(java.time.YearMonth.from(TODAY)))).size());
        assertEquals(2, app.transactions().search(TransactionQuery.all().withType(TransactionType.EXPENSE)).size());
    }
}
