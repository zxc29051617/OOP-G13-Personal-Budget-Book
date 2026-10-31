package budgetbook.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.util.function.Supplier;
import javax.swing.JLabel;

/** 顏色在繪製時才向 Palette 取得，切換深色模式後不用重建元件。 */
public class ThemedLabel extends JLabel {
    private transient Supplier<Color> color;

    public ThemedLabel(String text, Font font, Supplier<Color> color) {
        super(text);
        this.color = color;
        if (font != null) {
            setFont(font);
        }
    }

    public void setColor(Supplier<Color> color) {
        this.color = color;
        repaint();
    }

    @Override
    public Color getForeground() {
        return color == null ? super.getForeground() : color.get();
    }
}
