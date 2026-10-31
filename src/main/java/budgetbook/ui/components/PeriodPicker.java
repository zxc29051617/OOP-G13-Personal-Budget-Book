package budgetbook.ui.components;

import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.FlowLayout;
import java.awt.Font;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 「◀ 2026 年 10 月 ▶」月份（或年份）切換器。 */
public final class PeriodPicker extends JPanel {
    public enum Mode {
        MONTH,
        YEAR
    }

    private final YearMonth current;
    private final JLabel label = new ThemedLabel("", Fonts.of(Font.BOLD, 1.05f), Palette::text);
    private final List<Consumer<YearMonth>> listeners = new ArrayList<>();
    private YearMonth value;
    private Mode mode = Mode.MONTH;

    public PeriodPicker(YearMonth current) {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        this.current = current;
        this.value = current;
        JButton previous = Widgets.button("◀", () -> step(-1));
        previous.setToolTipText("上一期（Alt+←）");
        JButton next = Widgets.button("▶", () -> step(1));
        next.setToolTipText("下一期（Alt+→）");
        JButton reset = Widgets.link("回到本期", () -> setValue(current));
        add(previous);
        add(label);
        add(next);
        add(reset);
        updateLabel();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        updateLabel();
        fire();
    }

    public Mode mode() {
        return mode;
    }

    public YearMonth value() {
        return value;
    }

    public void setValue(YearMonth month) {
        value = month;
        updateLabel();
        fire();
    }

    public void step(int amount) {
        setValue(mode == Mode.MONTH ? value.plusMonths(amount) : value.plusYears(amount));
    }

    public void addListener(Consumer<YearMonth> listener) {
        listeners.add(listener);
    }

    public boolean isCurrent() {
        return mode == Mode.MONTH ? value.equals(current) : value.getYear() == current.getYear();
    }

    private void updateLabel() {
        label.setText(mode == Mode.MONTH ? value.getYear() + " 年 " + value.getMonthValue() + " 月" : value.getYear() + " 年");
    }

    private void fire() {
        listeners.forEach(listener -> listener.accept(value));
    }
}
