package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;

public class Ui {
    public static final Color BACKGROUND = new Color(245, 245, 239);
    public static final Color SURFACE = new Color(250, 249, 243);
    public static final Color CARD = new Color(255, 255, 250);
    public static final Color GREEN = new Color(93, 143, 103);
    public static final Color GREEN_DARK = new Color(49, 93, 63);
    public static final Color GREEN_SOFT = new Color(210, 235, 207);
    public static final Color MINT = new Color(226, 244, 226);
    public static final Color CREAM = new Color(250, 238, 203);
    public static final Color SAND = new Color(235, 224, 199);
    public static final Color TEXT = new Color(40, 47, 41);
    public static final Color MUTED = new Color(116, 123, 111);
    public static final Color LINE = new Color(224, 224, 214);
    public static final Color RED = new Color(196, 75, 75);
    public static final Color BLUE = new Color(76, 116, 156);
    public static final Color ORANGE = new Color(191, 139, 55);

    private Ui() {
    }

    public static JLabel appName() {
        JLabel label = new JLabel("個人記帳本");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel small(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        label.setForeground(MUTED);
        return label;
    }

    public static JPanel card() {
        RoundedPanel panel = new RoundedPanel(24, CARD);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JPanel tintedCard(Color color) {
        RoundedPanel panel = new RoundedPanel(24, color);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JPanel page() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        return panel;
    }

    public static void pad(Component component) {
        if (component instanceof JPanel panel) {
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        }
    }

    public static JButton primaryButton(String text) {
        return new RoundButton(text, GREEN_DARK, Color.WHITE);
    }

    public static JButton quietButton(String text) {
        return new RoundButton(text, new Color(238, 239, 230), TEXT);
    }

    public static JButton dangerButton(String text) {
        return new RoundButton(text, new Color(248, 228, 226), RED);
    }

    public static JButton navButton(String text) {
        JButton button = new RoundButton(text, new Color(0, 0, 0, 0), MUTED);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        button.setPreferredSize(new Dimension(54, 52));
        button.setMinimumSize(new Dimension(50, 52));
        return button;
    }

    public static void setNavSelected(JButton button, boolean selected) {
        if (button instanceof RoundButton roundButton) {
            roundButton.setButtonColors(selected ? GREEN_SOFT : new Color(0, 0, 0, 0), selected ? GREEN_DARK : MUTED);
        } else {
            button.setForeground(selected ? GREEN_DARK : MUTED);
            button.setBackground(selected ? GREEN_SOFT : new Color(0, 0, 0, 0));
        }
    }

    public static void styleInput(JTextField field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        field.setForeground(TEXT);
        field.setCaretColor(GREEN_DARK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(10, 12, 10, 12)));
    }

    public static void styleCombo(JComboBox<?> comboBox) {
        comboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        comboBox.setForeground(TEXT);
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createLineBorder(LINE));
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setOpaque(false);
        pane.setOpaque(false);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(42);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE);
        table.setSelectionBackground(GREEN_SOFT);
        table.setSelectionForeground(TEXT);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        table.setIntercellSpacing(new Dimension(0, 0));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        header.setBackground(SURFACE);
        header.setForeground(MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
    }

    public static class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;

        public RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        public void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(new Color(230, 230, 220));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundButton extends JButton {
        private Color backgroundColor;
        private Color foregroundColor;

        RoundButton(String text, Color backgroundColor, Color foregroundColor) {
            super(text);
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(11, 16, 11, 16));
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            setForeground(foregroundColor);
        }

        void setButtonColors(Color backgroundColor, Color foregroundColor) {
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            setForeground(foregroundColor);
            repaint();
        }

        @Override
        public Insets getInsets() {
            Insets insets = super.getInsets();
            return new Insets(insets.top, insets.left + 2, insets.bottom, insets.right + 2);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void removeOpaque(JComponent component) {
        component.setOpaque(false);
    }
}
