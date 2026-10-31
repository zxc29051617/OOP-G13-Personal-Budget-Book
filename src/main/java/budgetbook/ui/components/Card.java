package budgetbook.ui.components;

import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** 圓角卡片（延續原版的卡片風格），背景色依主題變化。 */
public class Card extends JPanel {
    private static final float ARC = 18f;

    public Card() {
        this(new BorderLayout(0, 10));
    }

    public Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    /** 標題 + 內容，可選擇在右上角放一個動作元件。 */
    public static Card titled(String title, JComponent action, JComponent body) {
        Card card = new Card();
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(Widgets.heading(title), BorderLayout.WEST);
        if (action != null) {
            header.add(action, BorderLayout.EAST);
        }
        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    public static Card titled(String title, JComponent body) {
        return titled(title, null, body);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC);
        g2.setColor(Palette.card());
        g2.fill(shape);
        g2.setColor(Palette.border());
        g2.draw(shape);
        g2.dispose();
        super.paintComponent(g);
    }
}
