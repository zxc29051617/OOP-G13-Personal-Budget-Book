package budgetbook.ui;

import budgetbook.Fixtures;
import budgetbook.model.CategoryKind;
import budgetbook.model.TransactionType;
import budgetbook.service.AppContext;
import budgetbook.ui.forms.CategoryForm;
import budgetbook.ui.forms.GoalForm;
import budgetbook.ui.forms.RecurringForm;
import budgetbook.ui.forms.TransactionForm;
import budgetbook.ui.forms.WalletForm;
import budgetbook.ui.pages.AssistantPage;
import budgetbook.ui.pages.TransactionsPage;
import budgetbook.ui.theme.Theme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 沒有螢幕時建立整個介面、切換每一頁並畫成圖片，確認不會丟出例外。 */
class HeadlessUiTest {
    private AppContext app;

    @BeforeEach
    void setUp() throws Exception {
        onEdt(() -> {
            Theme.apply(false);
            app = Fixtures.app();
            app.demo().loadDemoData();
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        onEdt(() -> {
            app.close();
            Theme.apply(false);
        });
    }

    private static void onEdt(Runnable action) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(action);
    }

    @Test
    void everyPageRendersInLightAndDarkTheme() throws Exception {
        onEdt(() -> {
            AppShell shell = new AppShell(app);
            for (boolean dark : List.of(false, true)) {
                shell.setDarkTheme(dark);
                for (Page page : shell.pages()) {
                    shell.show(page.id());
                    assertEquals(page, shell.current());
                    BufferedImage image = OffscreenRenderer.render(shell, 1200, 800);
                    assertTrue(OffscreenRenderer.countDistinctColors(image) > 20, page.id() + " looks blank");
                }
            }
            shell.dispose();
        });
    }

    @Test
    void freshInstallWithoutTransactionsRenders() throws Exception {
        onEdt(() -> {
            try (AppContext fresh = Fixtures.app()) {
                AppShell shell = new AppShell(fresh);
                for (Page page : shell.pages()) {
                    shell.show(page.id());
                    BufferedImage image = OffscreenRenderer.render(shell, 1200, 800);
                    assertTrue(OffscreenRenderer.countDistinctColors(image) > 10, page.id());
                }
                shell.dispose();
            }
        });
    }

    @Test
    void pagesRefreshWhenDataChanges() throws Exception {
        onEdt(() -> {
            AppShell shell = new AppShell(app);
            shell.show("transactions");
            long before = app.transactions().count();
            app.transactions().delete(app.transactions().recent(1).get(0).id());
            assertEquals(before - 1, app.transactions().count());
            app.commands().undo();
            assertEquals(before, app.transactions().count());
            assertTrue(app.commands().canRedo());
            shell.show("dashboard");
            shell.page(TransactionsPage.class).focusSearch();
            shell.page(AssistantPage.class).ask("本月花最多的是哪個分類？");
            shell.dispose();
        });
    }

    @Test
    void formsBuildWithoutAWindow() throws Exception {
        onEdt(() -> {
            List<FormDialog.Form> forms = List.of(
                    new TransactionForm(app, null, TransactionType.EXPENSE),
                    new TransactionForm(app, null, TransactionType.TRANSFER),
                    new TransactionForm(app, app.transactions().recent(1).get(0), TransactionType.EXPENSE),
                    new WalletForm(app, null),
                    new WalletForm(app, app.wallets().all().get(0)),
                    new CategoryForm(app, null, CategoryKind.EXPENSE),
                    new CategoryForm(app, app.categories().all().get(0), CategoryKind.EXPENSE),
                    new RecurringForm(app, null),
                    new RecurringForm(app, app.recurring().all().get(0)),
                    new GoalForm(app, null),
                    new GoalForm(app, app.goals().all().get(0)));
            for (FormDialog.Form form : forms) {
                JComponent content = form.content();
                BufferedImage image = OffscreenRenderer.render(content, 520, 420);
                assertTrue(OffscreenRenderer.countDistinctColors(image) > 3, form.title());
                assertTrue(form.initialFocus().isFocusable());
            }
        });
    }
}
