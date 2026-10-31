package budgetbook.ui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 兩欄表單：左邊是標籤、右邊是欄位。 */
public final class FormPanel extends JPanel {
    private int row;

    public FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    public <T extends JComponent> T add(String label, T field) {
        JLabel text = Widgets.label(label);
        text.setLabelFor(field);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(5, 0, 5, 12);
        super.add(text, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        super.add(field, c);
        return field;
    }

    /** 跨兩欄的元件，例如說明文字或錯誤訊息。 */
    public void addWide(JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 4, 0);
        super.add(component, c);
    }

    /** 欄位下方的小字說明。 */
    public void addHint(String text) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 4, 0);
        super.add(Widgets.muted(text), c);
    }
}
