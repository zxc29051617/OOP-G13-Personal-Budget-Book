package budgetbook.ui.components;

import budgetbook.model.Colors;
import budgetbook.ui.theme.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

/** 顏色選擇：常用色票，加上「自訂…」開啟完整的色彩選擇器。 */
public final class ColorPicker extends JPanel {
    private final List<JButton> swatches = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int color;

    public ColorPicker(int initial) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);
        for (int rgb : Colors.PALETTE) {
            JButton swatch = new JButton();
            swatch.putClientProperty("JButton.buttonType", "borderless");
            swatch.setToolTipText(Colors.toHex(rgb));
            swatch.addActionListener(e -> setColor(rgb));
            swatch.putClientProperty("rgb", rgb);
            swatches.add(swatch);
            add(swatch);
        }
        add(Widgets.link("自訂…", () -> {
            Color chosen = JColorChooser.showDialog(this, "選擇顏色", new Color(color));
            if (chosen != null) {
                setColor(chosen.getRGB() & 0xFFFFFF);
            }
        }));
        setColor(initial);
    }

    public int color() {
        return color;
    }

    public void setColor(int rgb) {
        color = rgb & 0xFFFFFF;
        for (JButton swatch : swatches) {
            int value = (Integer) swatch.getClientProperty("rgb");
            swatch.setIcon(new Swatch(value, value == color));
        }
        listeners.forEach(Runnable::run);
    }

    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    private record Swatch(int rgb, boolean selected) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(rgb));
            g2.fillOval(x + 3, y + 3, 16, 16);
            if (selected) {
                g2.setColor(Palette.text());
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 1, y + 1, 20, 20);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 22;
        }

        @Override
        public int getIconHeight() {
            return 22;
        }
    }
}
