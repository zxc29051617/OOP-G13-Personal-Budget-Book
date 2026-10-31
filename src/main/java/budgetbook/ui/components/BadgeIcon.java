package budgetbook.ui.components;

import budgetbook.model.Category;
import budgetbook.model.Wallet;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/** 圓形徽章圖示：分類或錢包的顏色加上一個字（或 emoji），延續原版的圓形徽章。 */
public final class BadgeIcon implements Icon {
    private final String glyph;
    private final Color color;
    private final int size;

    public BadgeIcon(String glyph, Color color, int size) {
        this.glyph = glyph == null ? "" : glyph;
        this.color = color;
        this.size = size;
    }

    public static BadgeIcon of(Category category, int size) {
        return new BadgeIcon(category.icon(), Palette.of(category.color()), size);
    }

    public static BadgeIcon of(Wallet wallet, int size) {
        return new BadgeIcon(wallet.icon(), Palette.of(wallet.color()), size);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(x, y, size, size);
        if (!glyph.isEmpty()) {
            int count = glyph.codePointCount(0, glyph.length());
            Font font = Fonts.of(Font.BOLD, 1f).deriveFont(Font.BOLD, size * (count > 1 ? 0.36f : 0.5f));
            g2.setFont(font);
            FontMetrics metrics = g2.getFontMetrics();
            int textX = x + (size - metrics.stringWidth(glyph)) / 2;
            int textY = y + (size - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.setColor(Palette.contrastText(color));
            g2.drawString(glyph, textX, textY);
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
