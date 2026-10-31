package budgetbook.ui.components;

import budgetbook.ui.theme.Palette;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.function.Supplier;
import javax.swing.JLabel;

/** 數字卡片：標題、大數字與一行說明，例如「本月支出 $23,180 比上月多 8%」。 */
public final class StatTile extends Card {
    private final ThemedLabel value;
    private final JLabel caption;

    public StatTile(String title, Supplier<Color> color) {
        super(new GridLayout(3, 1, 0, 2));
        add(Widgets.muted(title));
        value = Widgets.value("—", 1.5f, color);
        add(value);
        caption = Widgets.muted(" ");
        add(caption);
    }

    public void setValue(String text) {
        value.setText(text);
    }

    public void setValueColor(Supplier<Color> color) {
        value.setColor(color);
    }

    public void setCaption(String text) {
        caption.setText(text == null || text.isEmpty() ? " " : text);
    }

    public static StatTile accent(String title) {
        return new StatTile(title, Palette::text);
    }
}
