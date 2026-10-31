package budgetbook.ui.forms;

import budgetbook.model.Category;
import budgetbook.model.Wallet;
import budgetbook.ui.components.BadgeIcon;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/** 下拉選單中顯示分類、錢包的圖示。 */
public final class Renderers {
    private Renderers() {
    }

    public static DefaultListCellRenderer badges() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.setIconTextGap(8);
                if (value instanceof Category category) {
                    label.setIcon(BadgeIcon.of(category, 20));
                    label.setText(category.name());
                } else if (value instanceof Wallet wallet) {
                    label.setIcon(BadgeIcon.of(wallet, 20));
                    label.setText(wallet.name() + "（" + wallet.type().displayName() + "）");
                } else {
                    label.setIcon(null);
                }
                return label;
            }
        };
    }
}
