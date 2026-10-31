package budgetbook.ui.forms;

import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Frequency;
import budgetbook.model.RecurringRule;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.service.AppContext;
import budgetbook.ui.FormDialog;
import budgetbook.ui.components.Fields;
import budgetbook.ui.components.FormPanel;
import budgetbook.ui.components.Widgets;

import java.time.LocalDate;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** 新增或編輯定期交易，例如每月 1 日房租、每月 5 日薪資。 */
public final class RecurringForm implements FormDialog.Form {
    private final AppContext app;
    private final RecurringRule existing;
    private final JTextField name = new JTextField(16);
    private final JComboBox<TransactionType> type = new JComboBox<>(TransactionType.values());
    private final Fields.AmountField amount = new Fields.AmountField(null);
    private final JComboBox<Category> category = new JComboBox<>();
    private final JComboBox<Wallet> wallet = new JComboBox<>();
    private final JComboBox<Wallet> toWallet = new JComboBox<>();
    private final JComboBox<Frequency> frequency = new JComboBox<>(Frequency.values());
    private final JSpinner interval = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final Fields.DateField start;
    private final Fields.DateField end = new Fields.DateField(null);
    private final JTextField note = new JTextField(16);
    private final JCheckBox active = new JCheckBox("啟用（到期時自動入帳）", true);
    private final FormPanel form = new FormPanel();

    public RecurringForm(AppContext app, RecurringRule existing) {
        this.app = app;
        this.existing = existing;
        start = new Fields.DateField(existing == null ? LocalDate.now(app.clock()) : existing.startDate());
        category.setRenderer(Renderers.badges());
        wallet.setRenderer(Renderers.badges());
        toWallet.setRenderer(Renderers.badges());
        for (Wallet w : app.wallets().active()) {
            wallet.addItem(w);
            toWallet.addItem(w);
        }
        form.add("名稱", name);
        form.add("類型", type);
        form.add("金額", amount);
        form.add("分類", category);
        form.add("錢包", wallet);
        form.add("轉入錢包", toWallet);
        frequency.setSelectedItem(Frequency.MONTHLY);
        form.add("週期", Widgets.row(6, Widgets.label("每"), interval, frequency));
        form.add("開始日期", start);
        form.add("結束日期", end);
        form.addHint("結束日期可以空白；開始日期就是第一次入帳的日期。");
        form.add("備註", note);
        form.add("", active);
        type.addActionListener(e -> onTypeChanged());
        if (existing != null) {
            name.setText(existing.name());
            type.setSelectedItem(existing.type());
            amount.setAmount(existing.amount());
            selectWallet(wallet, existing.walletId());
            if (existing.toWalletId() != null) {
                selectWallet(toWallet, existing.toWalletId());
            }
            frequency.setSelectedItem(existing.frequency());
            interval.setValue(existing.interval());
            end.setDate(existing.endDate());
            note.setText(existing.note());
            active.setSelected(existing.active());
        }
        onTypeChanged();
        if (existing != null && existing.categoryId() != null) {
            for (int i = 0; i < category.getItemCount(); i++) {
                if (category.getItemAt(i).id() == existing.categoryId()) {
                    category.setSelectedIndex(i);
                }
            }
        }
    }

    private void onTypeChanged() {
        TransactionType selected = (TransactionType) type.getSelectedItem();
        boolean isTransfer = selected == TransactionType.TRANSFER;
        category.setEnabled(!isTransfer);
        toWallet.setEnabled(isTransfer);
        category.removeAllItems();
        if (!isTransfer) {
            app.categories().byKind(CategoryKind.of(selected)).forEach(category::addItem);
        }
    }

    private void selectWallet(JComboBox<Wallet> combo, long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.addItem(app.wallets().get(id));
        combo.setSelectedIndex(combo.getItemCount() - 1);
    }

    @Override
    public String title() {
        return existing == null ? "新增定期交易" : "編輯定期交易";
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
        TransactionType selected = (TransactionType) type.getSelectedItem();
        Wallet from = (Wallet) wallet.getSelectedItem();
        Wallet to = (Wallet) toWallet.getSelectedItem();
        Category chosen = (Category) category.getSelectedItem();
        RecurringRule rule = new RecurringRule(existing == null ? 0 : existing.id(), name.getText(), selected,
                amount.amount(), chosen == null ? null : chosen.id(), from == null ? 0 : from.id(),
                to == null ? null : to.id(), note.getText(), (Frequency) frequency.getSelectedItem(),
                (Integer) interval.getValue(), start.date("開始日期"), end.optionalDate("結束日期"),
                existing == null ? 0 : existing.postedCount(), active.isSelected());
        app.recurring().save(rule);
    }
}
