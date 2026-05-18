package app;

import persistence.BudgetBookStore;
import ui.BudgetBookFrame;
import ui.Dialogs;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class BudgetBookApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                applyModernLookAndFeel();
                BudgetBookStore store = new BudgetBookStore(Paths.get("data"));
                store.load();
                new BudgetBookFrame(store).setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                Dialogs.showError(null, "啟動失敗：" + e.getMessage());
            }
        });
    }

    private static void applyModernLookAndFeel() {
        try {
            UIManager.put("Button.arc", 18);
            UIManager.put("Component.arc", 18);
            UIManager.put("TextComponent.arc", 14);
            Class<?> flatLightLaf = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            Method setup = flatLightLaf.getMethod("setup");
            setup.invoke(null);
        } catch (Exception flatLafUnavailable) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing can continue with the default cross-platform look and feel.
            }
        }
    }
}
