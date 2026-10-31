package budgetbook.ui.components;

import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;
import org.knowm.xchart.internal.chartpart.Chart;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * 顯示 XChart 圖表的元件。直接呼叫 {@code chart.paint}，大小跟著版面變化，
 * 沒有資料時顯示提示文字。
 */
public final class ChartView extends JComponent {
    private transient Chart<?, ?> chart;
    private String emptyText = "沒有資料";
    private final int preferredHeight;

    public ChartView(int preferredHeight) {
        this.preferredHeight = preferredHeight;
        setOpaque(false);
    }

    public void setChart(Chart<?, ?> chart) {
        this.chart = chart;
        repaint();
    }

    public void showEmpty(String text) {
        this.chart = null;
        this.emptyText = text;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, preferredHeight);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(160, Math.min(preferredHeight, 160));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (chart != null && getWidth() > 20 && getHeight() > 20) {
            chart.paint(g2, getWidth(), getHeight());
        } else {
            g2.setFont(Fonts.small());
            g2.setColor(Palette.muted());
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(emptyText, (getWidth() - metrics.stringWidth(emptyText)) / 2,
                    getHeight() / 2 + metrics.getAscent() / 2);
        }
        g2.dispose();
    }
}
