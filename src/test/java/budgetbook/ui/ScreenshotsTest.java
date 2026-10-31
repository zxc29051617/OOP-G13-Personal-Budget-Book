package budgetbook.ui;

import budgetbook.repository.StorageConfig;
import budgetbook.service.AppContext;
import budgetbook.ui.pages.AssistantPage;
import budgetbook.ui.theme.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * 產生 README 用的截圖：{@code mvn test -Dtest=ScreenshotsTest -Dscreenshots=docs/images}。
 * 平常執行測試時會略過。
 */
@EnabledIfSystemProperty(named = "screenshots", matches = ".+")
class ScreenshotsTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Taipei");

    @Test
    void renderPages() throws Exception {
        Path dir = Path.of(System.getProperty("screenshots"));
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 10, 28, 14, 30).atZone(ZONE).toInstant(), ZONE);
        SwingUtilities.invokeAndWait(() -> {
            try (AppContext app = AppContext.open(StorageConfig.inMemory(), clock)) {
                Theme.apply(false);
                app.demo().ensureStarterData();
                app.demo().loadDemoData();
                AppShell shell = new AppShell(app);
                for (String page : List.of("dashboard", "transactions", "wallets", "categories", "budgets",
                        "recurring", "goals", "reports", "forest", "settings")) {
                    shell.show(page);
                    OffscreenRenderer.save(OffscreenRenderer.render(shell, 1360, 860), dir.resolve(page + ".png"));
                }
                shell.show("assistant");
                AssistantPage assistant = shell.page(AssistantPage.class);
                assistant.ask("這個月餐飲花了多少？");
                assistant.ask("預算還剩多少？");
                assistant.ask("跟上個月比，支出增加了嗎？");
                OffscreenRenderer.save(OffscreenRenderer.render(shell, 1360, 860), dir.resolve("assistant.png"));

                budgetbook.model.Transaction lunch = app.transactions().search(budgetbook.model.TransactionQuery.all()
                        .withText("牛肉麵")).get(0);
                javax.swing.JPanel dialog = new javax.swing.JPanel(new java.awt.BorderLayout(0, 12));
                dialog.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 20, 18, 20));
                dialog.add(budgetbook.ui.components.Widgets.heading("編輯交易"), java.awt.BorderLayout.NORTH);
                dialog.add(new budgetbook.ui.forms.TransactionForm(app, lunch, lunch.type()).content(),
                        java.awt.BorderLayout.CENTER);
                dialog.add(budgetbook.ui.components.Widgets.row(8,
                        budgetbook.ui.components.Widgets.button("取消", () -> { }),
                        budgetbook.ui.components.Widgets.primary("儲存", () -> { })), java.awt.BorderLayout.SOUTH);
                OffscreenRenderer.save(OffscreenRenderer.render(dialog, 560, 430), dir.resolve("transaction-form.png"));

                shell.setDarkTheme(true);
                shell.show("dashboard");
                OffscreenRenderer.save(OffscreenRenderer.render(shell, 1360, 860), dir.resolve("dashboard-dark.png"));
                Theme.apply(false);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
