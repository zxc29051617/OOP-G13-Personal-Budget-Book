package budgetbook.ui.components;

import budgetbook.model.Money;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.function.IntFunction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/** 表格的共用樣式與儲存格繪製器。 */
public final class Tables {
    private Tables() {
    }

    /** 圖示 + 文字的儲存格內容，可依文字排序。 */
    public record Labeled(Icon icon, String text) implements Comparable<Labeled> {
        @Override
        public int compareTo(Labeled other) {
            return text.compareTo(other.text);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static void style(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Fonts.of(Font.BOLD, 0.93f));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultRenderer(Labeled.class, new LabeledRenderer());
    }

    /** 金額靠右；顏色由每一列決定（收入綠、支出紅、轉帳藍）。 */
    public static DefaultTableCellRenderer moneyRenderer(IntFunction<Color> colorForModelRow, boolean signed) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                Object text = value instanceof Money money ? (signed ? money.formatSigned() : money.format()) : value;
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, text, selected, focused, row, column);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                label.setFont(Fonts.bold());
                label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 14));
                if (!selected && colorForModelRow != null) {
                    label.setForeground(colorForModelRow.apply(table.convertRowIndexToModel(row)));
                }
                return label;
            }
        };
    }

    /** 一般文字欄位，可指定顏色（例如類型欄）。 */
    public static DefaultTableCellRenderer textRenderer(IntFunction<Color> colorForModelRow) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
                if (!selected) {
                    label.setForeground(colorForModelRow == null ? Palette.text()
                            : colorForModelRow.apply(table.convertRowIndexToModel(row)));
                }
                return label;
            }
        };
    }

    private static final class LabeledRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
            label.setIconTextGap(8);
            if (value instanceof Labeled labeled) {
                label.setIcon(labeled.icon());
                label.setText(labeled.text());
            } else {
                label.setIcon(null);
            }
            return label;
        }
    }
}
