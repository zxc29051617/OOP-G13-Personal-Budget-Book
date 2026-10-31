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
