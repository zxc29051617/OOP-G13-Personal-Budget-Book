package budgetbook.ui.components;

import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/** 常用元件的工廠方法，讓各頁面的外觀一致。 */
public final class Widgets {
    private Widgets() {
    }

    public static JLabel title(String text) {
        return new ThemedLabel(text, Fonts.of(Font.BOLD, 1.55f), Palette::text);
    }

    public static JLabel heading(String text) {
        return new ThemedLabel(text, Fonts.of(Font.BOLD, 1.08f), Palette::text);
    }

    public static JLabel label(String text) {
        return new ThemedLabel(text, Fonts.regular(), Palette::text);
    }

    public static JLabel muted(String text) {
        return new ThemedLabel(text, Fonts.small(), Palette::muted);
    }

    public static ThemedLabel value(String text, float scale, Supplier<Color> color) {
        return new ThemedLabel(text, Fonts.of(Font.BOLD, scale), color);
    }

    public static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        button.setFocusable(true);
        return button;
    }

    /** 主要動作按鈕（綠色）。 */
    public static JButton primary(String text, Runnable action) {
        JButton button = button(text, action);
        button.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor;"
                + "foreground:#FFFFFF;borderColor:$Component.accentColor;hoverBackground:darken($Component.accentColor,6%);"
                + "pressedBackground:darken($Component.accentColor,12%);font:bold");
        return button;
    }

    /** 沒有外框的文字按鈕，用在卡片右上角。 */
    public static JButton link(String text, Runnable action) {
        JButton button = button(text, action);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        button.putClientProperty(FlatClientProperties.STYLE, "foreground:$Component.accentColor");
        return button;
    }

    public static JPanel row(int gap, Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        panel.setOpaque(false);
        for (Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    public static JPanel column(int gap, Component... components) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JComponent c) {
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            panel.add(components[i]);
            if (i < components.length - 1 && gap > 0) {
                panel.add(Box.createVerticalStrut(gap));
            }
        }
        return panel;
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return pane;
    }

    /** 沒有資料時的提示。 */
    public static JComponent empty(String message) {
        JLabel label = muted(message);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(18, 8, 18, 8));
        return label;
    }

    /** 讓元件貼齊上方，不被拉高（例如卡片中的表單）。 */
    public static JPanel top(JComponent component) {
        JPanel holder = new JPanel(new java.awt.BorderLayout());
        holder.setOpaque(false);
        holder.add(component, java.awt.BorderLayout.NORTH);
        return holder;
    }

    public static <T extends JComponent> T pad(T component, int top, int left, int bottom, int right) {
        component.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return component;
    }
}
