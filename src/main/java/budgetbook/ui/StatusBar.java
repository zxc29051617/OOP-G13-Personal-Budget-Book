package budgetbook.ui;

import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Graphics;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 視窗底部的狀態列：左邊顯示最近的操作，右邊顯示資料庫位置。 */
final class StatusBar extends JPanel {
    private final JLabel message = Widgets.muted(" ");
    private final Timer clear = new Timer(10_000, e -> message.setText(" "));

    StatusBar(String storageText) {
        super(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        add(message, BorderLayout.CENTER);
        add(Widgets.muted(storageText), BorderLayout.EAST);
        clear.setRepeats(false);
    }

    void show(String text) {
        message.setText(text);
        clear.restart();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Palette.sidebar());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Palette.border());
        g.drawLine(0, 0, getWidth(), 0);
    }
}
