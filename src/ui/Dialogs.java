package ui;

import java.awt.Component;
import javax.swing.JOptionPane;

public class Dialogs {
    private Dialogs() {
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "錯誤", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "完成", JOptionPane.INFORMATION_MESSAGE);
    }
}
