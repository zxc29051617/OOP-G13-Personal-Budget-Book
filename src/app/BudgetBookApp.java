package app;

import persistence.BudgetBookStore;
import ui.BudgetBookFrame;
import ui.Dialogs;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Paths;

public class BudgetBookApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                BudgetBookStore store = new BudgetBookStore(Paths.get("data"));
                store.load();
                new BudgetBookFrame(store).setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                Dialogs.showError(null, "啟動失敗：" + e.getMessage());
            }
        });
    }
}
