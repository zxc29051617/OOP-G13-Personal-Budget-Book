package budgetbook.service.report;

import budgetbook.model.BudgetStatus;
import budgetbook.model.CategoryTotal;
import budgetbook.model.Colors;
import budgetbook.model.PeriodSummary;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** 把報表輸出成單一 HTML 檔，用瀏覽器開啟即可列印或另存 PDF。 */
public final class HtmlReportExporter implements ReportExporter {
    @Override
    public String displayName() {
        return "HTML 網頁";
    }

    @Override
    public String extension() {
        return "html";
    }

    @Override
    public void write(Report report, Writer out) throws IOException {
        PeriodSummary s = report.summary();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-Hant\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<title>").append(escape(report.title())).append("</title>\n<style>\n")
                .append("body{font-family:'Noto Sans TC','Microsoft JhengHei',sans-serif;margin:32px auto;")
                .append("max-width:860px;color:#283028;padding:0 16px}h1{color:#315D3F}")
                .append("table{border-collapse:collapse;width:100%;margin:12px 0 28px}")
                .append("th,td{border-bottom:1px solid #e0e0d6;padding:6px 8px;text-align:left}")
                .append("td.num{text-align:right;font-variant-numeric:tabular-nums}")
                .append(".bar{height:10px;border-radius:5px;background:#e9e9e0}")
                .append(".fill{height:10px;border-radius:5px}.cards{display:flex;gap:12px;flex-wrap:wrap}")
                .append(".card{flex:1;min-width:150px;background:#f3f6f0;border-radius:12px;padding:12px 16px}")
                .append(".card b{display:block;font-size:22px;margin-top:4px}\n</style>\n</head>\n<body>\n");
        html.append("<h1>").append(escape(report.title())).append("</h1>\n<p>")
                .append(report.period().range().start()).append(" ~ ").append(report.period().range().end())
                .append("</p>\n<div class=\"cards\">");
        card(html, "收入", s.income().format());
        card(html, "支出", s.expense().format());
        card(html, "結餘", s.net().format());
        card(html, "儲蓄率", CsvReportExporter.percent(s.savingsRate()));
        html.append("</div>\n");
        report.expenseChange().ifPresent(change -> html.append("<p>支出與上期相比：")
                .append(CsvReportExporter.signedPercent(change)).append("</p>\n"));

        categories(html, "支出分類", report.expenseByCategory());
        categories(html, "收入分類", report.incomeByCategory());

        if (!report.budgets().isEmpty()) {
            html.append("<h2>預算</h2>\n<table><tr><th>預算</th><th>上限</th><th>已用</th><th>使用率</th><th>狀態</th></tr>\n");
            for (BudgetStatus b : report.budgets()) {
                html.append("<tr><td>").append(escape(b.label())).append("</td><td class=\"num\">")
                        .append(b.limit().format()).append("</td><td class=\"num\">").append(b.spent().format())
                        .append("</td><td class=\"num\">").append(b.percent()).append("%</td><td>")
                        .append(b.level().displayName()).append("</td></tr>\n");
            }
            html.append("</table>\n");
        }

        html.append("<h2>最大支出</h2>\n<table><tr><th>日期</th><th>分類</th><th>錢包</th><th>備註</th><th>金額</th></tr>\n");
        for (Report.Item item : report.topExpenses()) {
            html.append("<tr><td>").append(item.date()).append("</td><td>").append(escape(item.category()))
                    .append("</td><td>").append(escape(item.wallet())).append("</td><td>").append(escape(item.note()))
                    .append("</td><td class=\"num\">").append(item.amount().format()).append("</td></tr>\n");
        }
        html.append("</table>\n<p style=\"color:#777\">個人記帳本 產生於 ")
                .append(report.generatedAt().toString().replace('T', ' ')).append("</p>\n</body>\n</html>\n");
        out.write(html.toString());
        out.flush();
    }

    private static void card(StringBuilder html, String label, String value) {
        html.append("<div class=\"card\">").append(label).append("<b>").append(escape(value)).append("</b></div>");
    }

    private static void categories(StringBuilder html, String title, List<CategoryTotal> totals) {
        if (totals.isEmpty()) {
            return;
        }
        html.append("<h2>").append(title).append("</h2>\n<table><tr><th>分類</th><th>金額</th><th>筆數</th><th style=\"width:40%\">占比</th></tr>\n");
        for (CategoryTotal total : totals) {
            html.append("<tr><td>").append(escape(total.category().name())).append("</td><td class=\"num\">")
                    .append(total.total().format()).append("</td><td class=\"num\">").append(total.count())
                    .append("</td><td><div class=\"bar\"><div class=\"fill\" style=\"width:")
                    .append(total.sharePercent()).append("%;background:")
                    .append(Colors.toHex(total.category().color())).append("\"></div></div> ")
                    .append(total.sharePercent()).append("%</td></tr>\n");
        }
        html.append("</table>\n");
    }

    static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
