package budgetbook.repository;

import budgetbook.Fixtures;
import budgetbook.model.BudgetStatus;
import budgetbook.model.Category;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.repository.jdbc.SchemaMigrator;
import budgetbook.service.AppContext;
import budgetbook.service.io.ImportResult;
import budgetbook.service.report.MonthlyPeriod;
import budgetbook.service.report.Report;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.StringReader;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 對真正的 MySQL 執行整套流程。設定環境變數 {@code BUDGETBOOK_TEST_MYSQL_URL}
 * （以及 {@code BUDGETBOOK_TEST_MYSQL_USER}、{@code BUDGETBOOK_TEST_MYSQL_PASSWORD}）才會執行。
 */
@EnabledIfEnvironmentVariable(named = "BUDGETBOOK_TEST_MYSQL_URL", matches = ".+")
class MySqlStorageTest {
    private static StorageConfig config() {
        String user = System.getenv().getOrDefault("BUDGETBOOK_TEST_MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("BUDGETBOOK_TEST_MYSQL_PASSWORD", "");
        return StorageConfig.mysql(System.getenv("BUDGETBOOK_TEST_MYSQL_URL"), user, password);
    }

    @Test
    void fullScenarioWorksOnMySql() throws Exception {
        try (AppContext app = AppContext.open(config(), Fixtures.clock())) {
            assertEquals("MySQL", app.storage().info().backend());
            assertEquals(SchemaMigrator.latestVersion(), app.storage().info().schemaVersion());
            app.demo().loadDemoData();
            long count = app.transactions().count();
            assertTrue(count > 100);

            YearMonth month = YearMonth.from(Fixtures.TODAY);
            List<BudgetStatus> budgets = app.budgets().statuses(month);
            assertFalse(budgets.isEmpty());
            Report report = app.reports().generate(new MonthlyPeriod(month));
            assertEquals(report.summary(), app.statistics().summary(DateRange.ofMonth(month)));

            // 日期、時間、布林值、LIKE 跳脫與 UNION 加總在 MySQL 上也要正確
            Transaction latest = app.transactions().recent(1).get(0);
            assertEquals(latest, app.transactions().get(latest.id()));
            assertFalse(app.transactions().search(TransactionQuery.all().withText("便當")).isEmpty());
            assertTrue(app.transactions().search(TransactionQuery.all().withText("100%_x")).isEmpty());
            assertEquals(Money.sum(app.wallets().balances().values().stream()), app.wallets().totalBalance());
            app.settings().setDarkTheme(true);
            assertTrue(app.settings().isDarkTheme());
            app.settings().setDarkTheme(false);

            app.transactions().delete(latest.id());
            app.commands().undo();
            assertEquals(latest, app.transactions().get(latest.id()));

            Category food = app.categories().all().stream().filter(c -> c.name().equals("餐飲")).findFirst().orElseThrow();
            String csv = app.csv().exportToString(app.transactions().search(TransactionQuery.all().withCategory(food.id())));
            ImportResult again = app.csv().importCsv(new StringReader(csv));
            assertEquals(0, again.imported());
            assertTrue(again.skippedDuplicates() > 0);

            assertEquals(count, app.transactions().count());
            app.storage().clearAllData();
        }
        // 重新開啟時不會重複執行 migration
        try (Storage storage = StorageFactory.open(config())) {
            assertEquals(SchemaMigrator.latestVersion(), storage.info().schemaVersion());
            assertEquals(0, storage.transactions().count());
        }
    }
}
