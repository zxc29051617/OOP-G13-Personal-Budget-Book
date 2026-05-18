package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import service.StatsService;

public class TreeGardenPanel extends Ui.RoundedPanel {
    private int waterCount;
    private String status = "枯萎";

    public TreeGardenPanel() {
        super(24, Ui.GREEN_SOFT);
        setPreferredSize(new Dimension(0, 170));
        setMinimumSize(new Dimension(0, 150));
    }

    public void setTreeState(int waterCount, String status) {
        this.waterCount = Math.max(0, waterCount);
        this.status = status;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int centerX = width / 2;
        int baseY = getHeight() - 34;
        int progress = Math.min(StatsService.MONTHLY_TREE_GOAL, waterCount);
        float ratio = progress / (float) StatsService.MONTHLY_TREE_GOAL;

        g2.setColor(new Color(182, 158, 107));
        g2.fillRoundRect(centerX - 68, baseY, 136, 14, 14, 14);

        if (waterCount == 0) {
            drawWitheredTree(g2, centerX, baseY);
        } else {
            drawGrowingTree(g2, centerX, baseY, ratio);
        }

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g2.setColor(Ui.TEXT);
        g2.drawString(status, 18, 28);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.setColor(Ui.MUTED);
        g2.drawString("本月澆水 " + waterCount + " / " + StatsService.MONTHLY_TREE_GOAL + " 次", 18, 48);

        g2.dispose();
    }

    private void drawWitheredTree(Graphics2D g2, int centerX, int baseY) {
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(132, 98, 63));
        g2.drawLine(centerX, baseY, centerX, baseY - 58);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(centerX, baseY - 42, centerX - 30, baseY - 68);
        g2.drawLine(centerX, baseY - 48, centerX + 30, baseY - 74);
        g2.drawLine(centerX - 9, baseY - 56, centerX - 42, baseY - 58);
        g2.setColor(Ui.MUTED);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.drawString("今天還沒澆水", centerX - 44, baseY - 86);
    }

    private void drawGrowingTree(Graphics2D g2, int centerX, int baseY, float ratio) {
        int trunkHeight = 46 + Math.round(28 * ratio);
        int canopy = 30 + Math.round(34 * ratio);

        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(122, 86, 50));
        g2.drawLine(centerX, baseY, centerX, baseY - trunkHeight);

        g2.setColor(ratio >= 1 ? Ui.GREEN_DARK : Ui.GREEN);
        g2.fillOval(centerX - canopy, baseY - trunkHeight - canopy, canopy * 2, canopy * 2);
        g2.fillOval(centerX - canopy - 26, baseY - trunkHeight - canopy + 18, canopy + 18, canopy + 18);
        g2.fillOval(centerX + 8, baseY - trunkHeight - canopy + 16, canopy + 22, canopy + 22);

        if (ratio >= 1) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g2.setColor(Ui.GREEN_DARK);
            g2.drawString("本月完成", centerX - 30, baseY - trunkHeight - canopy - 10);
        }
    }
}
