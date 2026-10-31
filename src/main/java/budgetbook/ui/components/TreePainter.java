package budgetbook.ui.components;

import budgetbook.model.TreeStage;
import budgetbook.ui.theme.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * 依成長階段畫出小樹（純 Java2D 向量繪圖，任何大小都清晰）。
 * 延續原版 TreeGardenPanel 的造型：土堆、樹幹與三團樹冠，再細分成六個階段。
 */
public final class TreePainter {
    private static final Color TRUNK = new Color(0x8B5E3C);
    private static final Color DRY = new Color(0x9C7B5B);
    private static final Color LEAF_LIGHT = new Color(0x9BD18B);
    private static final Color LEAF = new Color(0x5AAE61);
    private static final Color LEAF_DEEP = new Color(0x3F8A57);
    private static final Color FRUIT = new Color(0xE76F51);

    private TreePainter() {
    }

    /** 在 (x, y, w, h) 的範圍內畫樹，樹底部貼齊範圍下緣。 */
    public static void paint(Graphics2D g, int x, int y, int w, int h, TreeStage stage) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double s = Math.min(w, h) / 100.0;
        double cx = x + w / 2.0;
        double ground = y + h - 8 * s;

        Color soil = Palette.isDark() ? new Color(0x6B5A44) : new Color(0xC9AE82);
        g2.setColor(soil);
        g2.fill(new Ellipse2D.Double(cx - 34 * s, ground - 5 * s, 68 * s, 12 * s));

        switch (stage) {
            case WITHERED -> withered(g2, cx, ground, s);
            case SEED -> seed(g2, cx, ground, s);
            case SPROUT -> sprout(g2, cx, ground, s);
            case SAPLING -> tree(g2, cx, ground, s, 26, 3.2, 13, LEAF_LIGHT, 1, false);
            case YOUNG_TREE -> tree(g2, cx, ground, s, 34, 4.2, 17, LEAF, 3, false);
            case FULL_TREE -> tree(g2, cx, ground, s, 40, 5.5, 21, LEAF_DEEP, 5, true);
        }
        g2.dispose();
    }

    private static void withered(Graphics2D g, double cx, double ground, double s) {
        g.setColor(DRY);
        g.setStroke(new BasicStroke((float) (4.5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D trunk = new Path2D.Double();
        trunk.moveTo(cx, ground);
        trunk.curveTo(cx - 2 * s, ground - 18 * s, cx + 4 * s, ground - 30 * s, cx + 1 * s, ground - 44 * s);
        g.draw(trunk);
        g.setStroke(new BasicStroke((float) (2.6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        line(g, cx + 1 * s, ground - 26 * s, cx - 16 * s, ground - 40 * s);
        line(g, cx + 2 * s, ground - 33 * s, cx + 17 * s, ground - 46 * s);
        line(g, cx - 9 * s, ground - 33 * s, cx - 20 * s, ground - 32 * s);
        g.setColor(new Color(0xB08D57));
        g.fill(new Ellipse2D.Double(cx - 26 * s, ground - 3 * s, 7 * s, 3.5 * s));
        g.fill(new Ellipse2D.Double(cx + 16 * s, ground - 2 * s, 6 * s, 3 * s));
    }

    private static void seed(Graphics2D g, double cx, double ground, double s) {
        g.setColor(new Color(0x8B6B4A));
        g.fill(new Ellipse2D.Double(cx - 5 * s, ground - 9 * s, 10 * s, 7 * s));
        g.setColor(LEAF_LIGHT);
        g.setStroke(new BasicStroke((float) (2 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        line(g, cx, ground - 8 * s, cx + 1 * s, ground - 14 * s);
    }

    private static void sprout(Graphics2D g, double cx, double ground, double s) {
        g.setColor(LEAF);
        g.setStroke(new BasicStroke((float) (2.6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        line(g, cx, ground, cx, ground - 22 * s);
        leaf(g, cx, ground - 20 * s, s, -35);
        leaf(g, cx, ground - 15 * s, s, 35);
    }

    private static void leaf(Graphics2D g, double x, double y, double s, double degrees) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.toRadians(degrees));
        g.setColor(LEAF_LIGHT);
        g.fill(new Ellipse2D.Double(degrees < 0 ? -15 * s : 1 * s, -4 * s, 14 * s, 8 * s));
        g.setTransform(old);
    }

    private static void tree(Graphics2D g, double cx, double ground, double s, double trunkHeight, double trunkWidth,
                             double crown, Color leaves, int clusters, boolean fruits) {
        g.setColor(TRUNK);
        g.setStroke(new BasicStroke((float) (trunkWidth * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        line(g, cx, ground, cx, ground - trunkHeight * s);
        double top = ground - trunkHeight * s;
        double r = crown * s;
        g.setColor(leaves);
        g.fill(new Ellipse2D.Double(cx - r, top - r * 1.25, r * 2, r * 2));
        if (clusters >= 3) {
            g.fill(new Ellipse2D.Double(cx - r * 1.65, top - r * 0.55, r * 1.5, r * 1.5));
            g.fill(new Ellipse2D.Double(cx + r * 0.15, top - r * 0.6, r * 1.55, r * 1.55));
        }
        if (clusters >= 5) {
            g.fill(new Ellipse2D.Double(cx - r * 1.2, top - r * 1.75, r * 1.35, r * 1.35));
            g.fill(new Ellipse2D.Double(cx - r * 0.1, top - r * 1.85, r * 1.4, r * 1.4));
        }
        g.setColor(Palette.mix(leaves, Color.WHITE, 0.25));
        g.fill(new Ellipse2D.Double(cx - r * 0.55, top - r * 1.05, r * 0.55, r * 0.4));
        if (fruits) {
            g.setColor(FRUIT);
            double d = 5.5 * s;
            g.fill(new Ellipse2D.Double(cx - r * 0.9, top - r * 0.2, d, d));
            g.fill(new Ellipse2D.Double(cx + r * 0.7, top - r * 0.05, d, d));
            g.fill(new Ellipse2D.Double(cx + r * 0.1, top - r * 1.3, d, d));
            g.fill(new Ellipse2D.Double(cx - r * 0.35, top + r * 0.35, d, d));
        }
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
    }
}
