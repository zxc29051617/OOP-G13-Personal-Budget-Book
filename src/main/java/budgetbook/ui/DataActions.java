package budgetbook.ui;

import budgetbook.exception.BudgetBookException;
import budgetbook.model.Transaction;
import budgetbook.service.io.ImportPreview;
import budgetbook.service.io.ImportResult;
import budgetbook.service.report.Report;
import budgetbook.service.report.ReportExporter;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 匯入匯出與示範資料等需要選檔或確認的動作，選單、頁面與設定頁共用。 */
public final class DataActions {
    private DataActions() {
    }

    public static void exportTransactions(UiContext ui, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            ui.showInfo("匯出 CSV", "目前的篩選條件下沒有交易可以匯出。");
            return;
        }
        chooseSaveFile(ui, "交易紀錄-" + LocalDate.now(ui.app().clock()) + ".csv", "csv", "CSV 試算表")
                .ifPresent(path -> {
                    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                        ui.app().csv().export(transactions, writer);
                        ui.status("已匯出 " + transactions.size() + " 筆交易到 " + path.getFileName());
                    } catch (IOException e) {
                        ui.showError("無法寫入檔案：" + e.getMessage());
                    }
                });
    }

    public static void importTransactions(UiContext ui) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("匯入交易 CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 試算表（*.csv）", "csv"));
        if (chooser.showOpenDialog(ui.window()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ImportPreview preview = ui.app().csv().preview(reader);
            StringBuilder message = new StringBuilder("「").append(path.getFileName()).append("」中有 ")
                    .append(preview.rows().size()).append(" 筆交易：\n・新的 ").append(preview.newCount())
                    .append(" 筆會匯入\n・與現有資料重複 ").append(preview.duplicateCount()).append(" 筆會略過");
            if (!preview.errors().isEmpty()) {
                message.append("\n・格式錯誤 ").append(preview.errors().size()).append(" 列不會匯入：");
                preview.errors().stream().limit(5).forEach(error -> message.append("\n　").append(error));
            }
            message.append("\n\n不存在的錢包與分類會自動建立。匯入後可以按 Ctrl+Z 整批復原。");
            if (preview.newCount() == 0) {
                ui.showInfo("匯入 CSV", message.toString());
                return;
            }
            if (ui.confirm("匯入 CSV", message.toString())) {
                ImportResult result = ui.app().csv().importRows(preview);
                ui.status(result.summary());
            }
        } catch (IOException e) {
            ui.showError("無法讀取檔案：" + e.getMessage());
        } catch (BudgetBookException e) {
            ui.showError("無法匯入：" + e.getMessage());
        }
    }

    public static void exportReport(UiContext ui, Report report, ReportExporter exporter) {
        chooseSaveFile(ui, report.title().replace(' ', '-') + "." + exporter.extension(), exporter.extension(),
                exporter.displayName()).ifPresent(path -> {
                    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                        exporter.write(report, writer);
                        ui.status("已匯出「" + report.title() + "」到 " + path.getFileName());
                    } catch (IOException e) {
                        ui.showError("無法寫入檔案：" + e.getMessage());
                    }
                });
    }

    public static void loadDemoData(UiContext ui) {
        if (ui.confirm("載入示範資料", "這會清除目前所有的錢包、分類、交易、預算、定期交易與儲蓄目標，"
                + "並載入最近六個月的示範資料。\n這個動作無法復原，確定要繼續嗎？")) {
            ui.run(() -> {
                ui.app().demo().loadDemoData();
                ui.status("已載入示範資料。");
            });
        }
    }

    public static void resetData(UiContext ui) {
        if (ui.confirm("清除所有資料", "這會刪除所有的交易、錢包、分類、預算、定期交易與儲蓄目標，"
                + "只留下預設的分類與三個錢包。\n建議先用「匯出全部交易 CSV」備份。這個動作無法復原，確定要繼續嗎？")) {
            ui.run(() -> {
                ui.app().demo().resetToStarterData();
                ui.status("已清除所有資料，可以開始記自己的帳了。");
            });
        }
    }

    private static Optional<Path> chooseSaveFile(UiContext ui, String suggestedName, String extension, String description) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("另存新檔");
        chooser.setFileFilter(new FileNameExtensionFilter(description + "（*." + extension + "）", extension));
        chooser.setSelectedFile(new File(suggestedName));
        if (chooser.showSaveDialog(ui.window()) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith("." + extension)) {
            path = path.resolveSibling(path.getFileName() + "." + extension);
        }
        if (Files.exists(path) && !ui.confirm("另存新檔", "「" + path.getFileName() + "」已經存在，要覆蓋嗎？")) {
            return Optional.empty();
        }
        return Optional.of(path);
    }
}
