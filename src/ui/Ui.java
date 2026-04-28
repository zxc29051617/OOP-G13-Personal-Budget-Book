package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

public class Ui {
    public static final Color BACKGROUND = new Color(244, 247, 250);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(26, 33, 46);
    public static final Color MUTED = new Color(104, 116, 133);
    public static final Color GREEN = new Color(35, 145, 92);
    public static final Color RED = new Color(206, 76, 72);
    public static final Color BLUE = new Color(50, 104, 190);
    public static final Color TEAL = new Color(0, 136, 145);
    public static final Color SIDEBAR = new Color(29, 37, 52);
    public static final Color SIDEBAR_ITEM = new Color(46, 57, 77);
    public static final Color SIDEBAR_HOVER = new Color(59, 73, 98);

    private Ui() {
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel small(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        label.setForeground(MUTED);
        return label;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 228, 236)),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        return panel;
    }

    public static void pad(Component component) {
        if (component instanceof JPanel panel) {
            panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        }
    }

    public static JButton primaryButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);
        return button;
    }

    public static JButton quietButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(new Color(231, 236, 244));
        button.setForeground(TEXT);
        return button;
    }

    public static JButton navButton(String text) {
        JButton button = baseButton(text);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setBackground(SIDEBAR_ITEM);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(13, 16, 13, 16));
        return button;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(232, 236, 242));
        table.setSelectionBackground(new Color(220, 233, 255));
        table.setSelectionForeground(TEXT);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setBackground(new Color(237, 241, 247));
        header.setForeground(TEXT);
    }

    private static JButton baseButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        return button;
    }
}
