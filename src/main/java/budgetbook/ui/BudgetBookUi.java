package budgetbook.ui;

import budgetbook.exception.BudgetBookException;
import budgetbook.model.Transaction;
import budgetbook.repository.StorageConfig;
import budgetbook.service.AppContext;
import budgetbook.ui.theme.Theme;

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 啟動圖形介面：套用主題、開啟資料庫、補入帳定期交易，第一次使用時詢問是否載入示範資料。 */
public final class BudgetBookUi {
    private BudgetBookUi() {
    }

    /**
     * @param storage  使用者設定的資料庫
     * @param fallback 開啟失敗（例如 MySQL 沒有啟動）時可以改用的 SQLite
     * @param demo     示範模式：記憶體資料庫並載入示範資料
     */
    public static void launch(StorageConfig storage, StorageConfig fallback, boolean demo) {
        SwingUtilities.invokeLater(() -> {
            Theme.apply(false);
            AppContext app = open(storage, fallback);
            if (app == null) {
                System.exit(1);
                return;
            }
            if (app.settings().isDarkTheme()) {
                Theme.switchTo(true);
            }
            boolean firstRun = app.demo().ensureStarterData();
            if (demo) {
                app.demo().loadDemoData();
                app.settings().markWelcomed();
            }
            List<Transaction> posted = postDueRecurring(app);
            MainFrame frame = new MainFrame(app, demo);
            frame.setVisible(true);
            if (!posted.isEmpty()) {
                frame.shell().showStatus("已自動入帳 " + posted.size() + " 筆到期的定期交易（Ctrl+Z 可復原）");
            }
            if (firstRun && !app.settings().isWelcomed()) {
                welcome(frame, app);
            }
        });
    }

    private static AppContext open(StorageConfig storage, StorageConfig fallback) {
        try {
            return AppContext.open(storage);
        } catch (BudgetBookException e) {
            String message = "無法開啟資料庫（" + storage + "）：\n" + e.getMessage();
            if (fallback == null || fallback.equals(storage)) {
                JOptionPane.showMessageDialog(null, message, "個人記帳本", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            int choice = JOptionPane.showConfirmDialog(null, message + "\n\n要改用內建的 SQLite 資料庫（"
                    + fallback.describe() + "）嗎？", "個人記帳本", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return choice == JOptionPane.YES_OPTION ? open(fallback, null) : null;
        }
    }

    private static List<Transaction> postDueRecurring(AppContext app) {
        try {
            return app.recurring().postDue();
        } catch (BudgetBookException e) {
            JOptionPane.showMessageDialog(null, "定期交易自動入帳失敗：" + e.getMessage(), "個人記帳本",
                    JOptionPane.WARNING_MESSAGE);
            return List.of();
        }
    }

    private static void welcome(MainFrame frame, AppContext app) {
        Object[] options = {"載入示範資料", "從空白開始"};
        int choice = JOptionPane.showOptionDialog(frame, "歡迎使用個人記帳本！\n\n"
                        + "已建立預設的分類與「現金、銀行帳戶、電子支付」三個錢包。\n"
                        + "要先載入最近六個月的示範資料，看看圖表、預算與森林長什麼樣子嗎？\n"
                        + "（試用完可以在「設定 → 清除所有資料」重新開始記自己的帳。）",
                "歡迎", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        app.settings().markWelcomed();
        if (choice == 0) {
            app.demo().loadDemoData();
            frame.shell().showStatus("已載入示範資料。");
        }
    }
}
