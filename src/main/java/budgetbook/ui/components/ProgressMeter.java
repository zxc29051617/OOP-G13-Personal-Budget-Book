package budgetbook.ui.components;

import budgetbook.ui.theme.Palette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;
import javax.swing.JComponent;

/** 圓角進度條；比例超過 1 時整條填滿（超支）。 */
public final class ProgressMeter extends JComponent {
    private double ratio;
    private transient Supplier<Color> color = Palette::accent;
    private final int thickness;

    public ProgressMeter(int thickness) {
        this.thickness = thickness;
        setOpaque(false);
    }

    public ProgressMeter() {
        this(10);
    }

    public void setRatio(double ratio) {
        this.ratio = Math.max(0, ratio);
        repaint();
    }

    public void setColor(Supplier<Color> color) {
        this.color = color;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, thickness);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(40, thickness);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, thickness);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - thickness) / 2;
        g2.setColor(Palette.track());
        g2.fill(new RoundRectangle2D.Float(0, y, getWidth(), thickness, thickness, thickness));
        double shown = Math.min(1.0, ratio);
        if (shown > 0) {
            float width = (float) Math.max(thickness, getWidth() * shown);
            g2.setColor(color.get());
            g2.fill(new RoundRectangle2D.Float(0, y, width, thickness, thickness, thickness));
        }
        g2.dispose();
    }
}
