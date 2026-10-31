package budgetbook.ui.forms;

import budgetbook.model.Colors;
import budgetbook.model.Money;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.service.AppContext;
import budgetbook.ui.FormDialog;
import budgetbook.ui.components.ColorPicker;
import budgetbook.ui.components.Fields;
import budgetbook.ui.components.FormPanel;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

/** 新增或編輯錢包。 */
public final class WalletForm implements FormDialog.Form {
    private final AppContext app;
    private final Wallet existing;
    private final JTextField name = new JTextField(18);
    private final JComboBox<WalletType> type = new JComboBox<>(WalletType.values());
    private final Fields.AmountField initial = new Fields.AmountField(Money.ZERO);
    private final ColorPicker color;
    private final JCheckBox archived = new JCheckBox("封存（不出現在記帳選單，保留歷史紀錄）");
    private final FormPanel form = new FormPanel();

    public WalletForm(AppContext app, Wallet existing) {
        this.app = app;
        this.existing = existing;
        color = new ColorPicker(existing == null ? Colors.paletteColor(app.wallets().all().size() + 4) : existing.color());
        form.add("名稱", name);
        form.add("種類", type);
        form.add("初始金額", initial);
        form.addHint("建立錢包時裡面已經有的錢；信用卡可以輸入負數表示欠款。");
        form.add("顏色", color);
        if (existing != null) {
            name.setText(existing.name());
            type.setSelectedItem(existing.type());
            initial.setAmount(existing.initialBalance());
            archived.setSelected(existing.archived());
            form.add("", archived);
        }
    }

    @Override
    public String title() {
        return existing == null ? "新增錢包" : "編輯錢包";
    }

    @Override
    public JComponent content() {
        return form;
    }

    @Override
    public JComponent initialFocus() {
        return name;
    }

    @Override
    public void save() {
        WalletType selected = (WalletType) type.getSelectedItem();
        if (existing == null) {
            app.wallets().create(name.getText(), selected, initial.amount(), color.color());
        } else {
            app.wallets().update(new Wallet(existing.id(), name.getText(), selected, initial.amount(), color.color(),
                    archived.isSelected()));
        }
    }
}
