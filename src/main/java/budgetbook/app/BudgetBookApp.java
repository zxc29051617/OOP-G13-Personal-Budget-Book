package budgetbook.app;

import budgetbook.repository.StorageConfig;
import budgetbook.ui.BudgetBookUi;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;

/** 程式進入點：解析參數，執行 smoke test 或開啟視窗。 */
public final class BudgetBookApp {
    private BudgetBookApp() {
    }

    public static void main(String[] args) {
        AppConfig config;
        try {
            config = AppConfig.parse(args, System.getenv(), Path.of(System.getProperty("user.home")));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }
        if (config.help()) {
            System.out.println(AppConfig.USAGE);
            return;
        }
        if (config.smokeTest()) {
            runSmokeTest();
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("找不到圖形介面環境。沒有螢幕時可以使用 --smoke-test 檢查程式。");
            System.exit(2);
        }
        BudgetBookUi.launch(config.storage(), StorageConfig.sqlite(config.dataDir().resolve("budgetbook.db")),
                config.demo());
    }

    private static void runSmokeTest() {
        try {
            Path workDir = Files.createTempDirectory("budgetbook-smoke");
            SmokeTest.run(System.out, workDir);
            Files.deleteIfExists(workDir);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
