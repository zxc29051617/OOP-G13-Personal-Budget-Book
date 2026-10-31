package budgetbook.service.report;

import budgetbook.model.BudgetStatus;
import budgetbook.model.CategoryTotal;
import budgetbook.model.PeriodSummary;
import budgetbook.model.PeriodTotals;
import budgetbook.service.io.Csv;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

/** 把報表輸出成 CSV（UTF-8 含 BOM），用 Excel 或 Google 試算表開啟後可以再做圖。 */
public final class CsvReportExporter implements ReportExporter {
    @Override
    public String displayName() {
        return "CSV 試算表";
    }

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public void write(Report report, Writer out) throws IOException {
        out.write('﻿');
        PeriodSummary summary = report.summary();
        line(out, "報表", report.title());
        line(out, "期間", report.period().range().start().toString(), report.period().range().end().toString());
        line(out, "產生時間", report.generatedAt().toString().replace('T', ' '));
        out.write("\r\n");

        line(out, "項目", "金額");
        line(out, "收入", summary.income().toPlainString());
        line(out, "支出", summary.expense().toPlainString());
        line(out, "結餘", summary.net().toPlainString());
        line(out, "儲蓄率", percent(summary.savingsRate()));
        line(out, "交易筆數", Integer.toString(summary.transactionCount()));
        line(out, "上期支出", report.previousSummary().expense().toPlainString());
        line(out, "支出變化", report.expenseChange().map(CsvReportExporter::signedPercent).orElse(""));
        out.write("\r\n");

        categories(out, "支出分類", report.expenseByCategory());
        categories(out, "收入分類", report.incomeByCategory());

        line(out, report.period().seriesLabel(), "收入", "支出", "結餘");
        for (PeriodTotals totals : report.series()) {
            line(out, totals.label(), totals.income().toPlainString(), totals.expense().toPlainString(),
                    totals.net().toPlainString());
        }
        out.write("\r\n");

        if (!report.budgets().isEmpty()) {
            line(out, "預算", "上限", "已用", "剩餘", "使用率", "狀態");
            for (BudgetStatus status : report.budgets()) {
                line(out, status.label(), status.limit().toPlainString(), status.spent().toPlainString(),
                        status.remaining().toPlainString(), status.percent() + "%", status.level().displayName());
            }
            out.write("\r\n");
        }

        line(out, "最大支出", "分類", "錢包", "備註", "金額");
        for (Report.Item item : report.topExpenses()) {
            line(out, item.date().toString(), item.category(), item.wallet(), Csv.protectFormula(item.note()),
                    item.amount().toPlainString());
        }
        out.flush();
    }

    private static void categories(Writer out, String title, List<CategoryTotal> totals) throws IOException {
        line(out, title, "金額", "筆數", "占比");
        for (CategoryTotal total : totals) {
            line(out, total.category().name(), total.total().toPlainString(), Integer.toString(total.count()),
                    total.sharePercent() + "%");
        }
        out.write("\r\n");
    }

    private static void line(Writer out, String... values) throws IOException {
        out.write(Csv.row(List.of(values)));
        out.write("\r\n");
    }

    static String percent(double ratio) {
        return Math.round(ratio * 100) + "%";
    }

    static String signedPercent(double ratio) {
        return String.format(Locale.ROOT, "%+d%%", Math.round(ratio * 100));
    }
}
