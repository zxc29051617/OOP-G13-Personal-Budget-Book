package budgetbook.app;

import budgetbook.model.BudgetStatus;
import budgetbook.model.DateRange;
import budgetbook.model.Forest;
import budgetbook.model.PeriodSummary;
import budgetbook.model.Transaction;
import budgetbook.repository.StorageConfig;
import budgetbook.service.AppContext;
import budgetbook.service.io.ImportResult;
import budgetbook.service.report.CsvReportExporter;
import budgetbook.service.report.HtmlReportExporter;
import budgetbook.service.report.MonthlyPeriod;
import budgetbook.service.report.Report;
import budgetbook.service.report.YearlyPeriod;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

/**
 * 不需要螢幕的自我檢查（{@code --smoke-test}）：建立 SQLite 資料庫、載入示範資料，
 * 呼叫每一個 service，最後確認 CSV 匯出再匯入的筆數一致。任何一步失敗都會丟出例外。
 */
public final class SmokeTest {
    private SmokeTest() {
    }

    public static void run(PrintStream out, Path workDir) throws IOException {
        Path database = workDir.resolve("smoke-test.db");
        Files.deleteIfExists(database);
        out.println("個人記帳本 smoke test");
        try (AppContext app = AppContext.open(StorageConfig.sqlite(database))) {
            out.println("資料庫：" + app.storage().info().backend() + " " + app.storage().info().location()
                    + "（schema v" + app.storage().info().schemaVersion() + "）");
            check(app.demo().ensureStarterData(), "建立預設分類與錢包");
            app.demo().loadDemoData();
            long transactions = app.transactions().count();
            out.println("示範資料：" + app.wallets().all().size() + " 個錢包、" + app.categories().all().size()
                    + " 個分類、" + transactions + " 筆交易、" + app.recurring().all().size() + " 條定期交易、"
                    + app.goals().all().size() + " 個儲蓄目標");
            check(transactions > 100, "示範交易數量");

            YearMonth month = app.statistics().currentMonth();
            PeriodSummary summary = app.statistics().summary(DateRange.ofMonth(month));
            out.println("本月收入 " + summary.income().format() + "、支出 " + summary.expense().format()
                    + "、總資產 " + app.wallets().totalBalance().format());
            List<BudgetStatus> budgets = app.budgets().statuses(month);
            check(!budgets.isEmpty(), "預算狀態");
            budgets.forEach(b -> out.println("  預算 " + b.label() + " " + b.percent() + "% " + b.level().displayName()));
            Forest forest = app.trees().forest(12);
            out.println("森林：" + forest.trees().size() + " 個月、完成 " + forest.completedCount() + " 棵、連續記帳 "
                    + forest.currentStreak() + " 天");
            forest.trees().forEach(t -> out.println("  " + t.month() + " 澆水 " + t.records() + " 次 "
                    + t.stage().displayName()));

            Report monthly = app.reports().generate(new MonthlyPeriod(month));
            Report yearly = app.reports().generate(new YearlyPeriod(month.getYear()));
            String csvReport = app.reports().export(monthly, new CsvReportExporter());
            String htmlReport = app.reports().export(yearly, new HtmlReportExporter());
            check(csvReport.contains("支出分類") && htmlReport.contains("<table>"), "報表輸出");
            out.println("報表：月報 CSV " + csvReport.length() + " 字、年報 HTML " + htmlReport.length() + " 字");

            List<Transaction> all = app.transactions().search(budgetbook.model.TransactionQuery.all());
            String csv = app.csv().exportToString(all);
            try (AppContext copy = AppContext.open(StorageConfig.inMemory())) {
                ImportResult result = copy.csv().importCsv(new StringReader(csv));
                check(result.imported() == all.size() && result.errors().isEmpty(), "CSV 匯出後再匯入");
                ImportResult again = copy.csv().importCsv(new StringReader(csv));
                check(again.imported() == 0 && again.skippedDuplicates() == all.size(), "重複匯入會被略過");
                out.println("CSV：" + result.summary());
            }

            Transaction latest = app.transactions().recent(1).get(0);
            app.transactions().delete(latest.id());
            check(app.transactions().count() == transactions - 1, "刪除交易");
            app.commands().undo();
            check(app.transactions().count() == transactions, "復原刪除");
            app.commands().redo();
            app.commands().undo();
            check(app.transactions().get(latest.id()).equals(latest), "重做再復原");

            for (String question : List.of("本月花最多的是哪個分類？", "預算還剩多少？", "本月小樹長得怎樣？")) {
                out.println("問：" + question);
                out.println("答：" + app.assistant().answer(question).replace("\n", "\n    "));
            }
        }
        Files.deleteIfExists(database);
        out.println("OK：所有檢查都通過");
    }

    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("檢查失敗：" + what);
        }
    }
}
