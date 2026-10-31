package budgetbook.ui.forms;

import budgetbook.exception.ValidationException;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.service.AppContext;
import budgetbook.ui.FormDialog;
import budgetbook.ui.components.Fields;
import budgetbook.ui.components.FormPanel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Fonts;

import java.awt.Font;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 新增或編輯交易。備註會依歷史自動完成（類似 GnuCash 的 QuickFill），
 * 並依過去相同備註推薦分類；金額欄可以輸入算式。
 */
public final class TransactionForm implements FormDialog.Form {
    private final AppContext app;
    private final Transaction existing;
    private final List<String> recentNotes;
    private final JToggleButton expense = new JToggleButton("支出");
    private final JToggleButton income = new JToggleButton("收入");
    private final JToggleButton transfer = new JToggleButton("轉帳");
    private final Fields.AmountField amount = new Fields.AmountField(null);
    private final Fields.DateField date;
    private final Fields.TimeField time;
    private final JComboBox<Category> category = new JComboBox<>();
    private final JComboBox<Wallet> wallet = new JComboBox<>();
    private final JComboBox<Wallet> toWallet = new JComboBox<>();
    private final JTextField note = new JTextField(22);
    private final JLabel hint = Widgets.muted(" ");
    private final FormPanel form = new FormPanel();
    private JLabel walletLabel;
    private JComponent toWalletRow;
    private boolean categoryChosenByUser;
    private boolean completing;

    public TransactionForm(AppContext app, Transaction existing, TransactionType preset) {
        this.app = app;
        this.existing = existing;
        this.recentNotes = app.transactions().noteSuggestions(300);
        LocalDate today = LocalDate.now(app.clock());
        date = new Fields.DateField(existing == null ? today : existing.date());
        time = new Fields.TimeField(existing == null ? LocalTime.now(app.clock()) : existing.time());
        build();
        TransactionType type = existing == null ? preset : existing.type();
        (type == TransactionType.INCOME ? income : type == TransactionType.TRANSFER ? transfer : expense).setSelected(true);
        onTypeChanged();
        if (existing != null) {
            amount.setAmount(existing.amount());
            note.setText(existing.note());
            select(wallet, existing.walletId());
            if (existing.toWalletId() != null) {
                select(toWallet, existing.toWalletId());
            }
            if (existing.categoryId() != null) {
                selectCategory(existing.categoryId());
                categoryChosenByUser = true;
            }
        }
    }

    private void build() {
        ButtonGroup types = new ButtonGroup();
        for (JToggleButton button : List.of(expense, income, transfer)) {
            types.add(button);
            button.addActionListener(e -> onTypeChanged());
        }
        form.add("類型", Widgets.row(6, expense, income, transfer));
        amount.setFont(Fonts.of(Font.BOLD, 1.3f));
        form.add("金額", amount);
        form.add("日期", Widgets.row(6, date, time,
                Widgets.link("今天", () -> date.setDate(LocalDate.now(app.clock()))),
                Widgets.link("昨天", () -> date.setDate(LocalDate.now(app.clock()).minusDays(1)))));
        category.setRenderer(Renderers.badges());
        category.addActionListener(e -> {
            if (category.isPopupVisible() || category.hasFocus()) {
                categoryChosenByUser = true;
            }
        });
        form.add("分類", category);
        wallet.setRenderer(Renderers.badges());
        toWallet.setRenderer(Renderers.badges());
        form.add("錢包", wallet);
        toWalletRow = form.add("轉入錢包", toWallet);
        form.add("備註", note);
        form.addHint("備註會自動完成，並依過去的紀錄推薦分類。");
        form.addWide(hint);
        for (Wallet w : app.wallets().active()) {
            wallet.addItem(w);
            toWallet.addItem(w);
        }
        if (toWallet.getItemCount() > 1) {
            toWallet.setSelectedIndex(1);
        }
        walletLabel = findLabel(wallet);
        note.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!completing) {
                    SwingUtilities.invokeLater(TransactionForm.this::complete);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(TransactionForm.this::suggestCategory);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    private JLabel findLabel(JComponent field) {
        for (java.awt.Component c : form.getComponents()) {
            if (c instanceof JLabel label && label.getLabelFor() == field) {
                return label;
            }
        }
        return new JLabel();
    }

    private TransactionType type() {
        return income.isSelected() ? TransactionType.INCOME
                : transfer.isSelected() ? TransactionType.TRANSFER : TransactionType.EXPENSE;
    }

    private void onTypeChanged() {
        TransactionType type = type();
        boolean isTransfer = type == TransactionType.TRANSFER;
        category.setEnabled(!isTransfer);
        toWallet.setEnabled(isTransfer);
        toWalletRow.setVisible(isTransfer);
        findLabel(toWallet).setVisible(isTransfer);
        walletLabel.setText(isTransfer ? "轉出錢包" : "錢包");
        if (!isTransfer) {
            Category previous = (Category) category.getSelectedItem();
            category.removeAllItems();
            app.categories().byKind(CategoryKind.of(type)).forEach(category::addItem);
            if (previous != null && previous.kind() == CategoryKind.of(type)) {
                category.setSelectedItem(previous);
            }
            categoryChosenByUser = false;
            suggestCategory();
        }
        hint.setText(" ");
        SwingUtilities.invokeLater(() -> {
            if (SwingUtilities.getWindowAncestor(form) != null) {
                SwingUtilities.getWindowAncestor(form).pack();
            }
        });
    }

    /** 自動完成：輸入的文字是某個舊備註的開頭時，補上剩下的部分並反白。 */
    private void complete() {
        String text = note.getText();
        if (text.isEmpty() || note.getCaretPosition() != text.length()) {
            suggestCategory();
            return;
        }
        Optional<String> match = recentNotes.stream()
                .filter(n -> n.length() > text.length() && n.startsWith(text)).findFirst();
        match.ifPresent(full -> {
            completing = true;
            note.setText(full);
            note.select(text.length(), full.length());
            completing = false;
        });
        suggestCategory();
    }

    private void suggestCategory() {
        if (categoryChosenByUser || type() == TransactionType.TRANSFER) {
            return;
        }
        app.transactions().suggestCategory(note.getText(), type()).ifPresent(id -> {
            selectCategory(id);
            Category chosen = (Category) category.getSelectedItem();
            if (chosen != null) {
                hint.setText("依過去紀錄選擇了分類「" + chosen.name() + "」。");
            }
        });
    }

    private void selectCategory(long id) {
        for (int i = 0; i < category.getItemCount(); i++) {
            if (category.getItemAt(i).id() == id) {
                category.setSelectedIndex(i);
                return;
            }
        }
    }

    private void select(JComboBox<Wallet> combo, long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        // 已封存的錢包不在清單中，編輯舊交易時仍要能顯示
        combo.addItem(app.wallets().get(id));
        combo.setSelectedIndex(combo.getItemCount() - 1);
    }

    @Override
    public String title() {
        return existing == null ? "新增交易" : "編輯交易";
    }

    @Override
    public JComponent content() {
        return form;
    }

    @Override
    public JComponent initialFocus() {
        return amount;
    }

    @Override
    public void save() {
        Wallet from = (Wallet) wallet.getSelectedItem();
        if (from == null) {
            throw new ValidationException("錢包", "請先到「錢包」頁新增一個錢包。");
        }
        TransactionType type = type();
        Transaction.Builder builder = Transaction.builder()
                .id(existing == null ? 0 : existing.id())
                .type(type)
                .amount(amount.amount())
                .date(date.date("日期"))
                .time(time.time())
                .walletId(from.id())
                .note(note.getText())
                .recurringRuleId(existing == null ? null : existing.recurringRuleId());
        if (type == TransactionType.TRANSFER) {
            Wallet to = (Wallet) toWallet.getSelectedItem();
            builder.toWalletId(to == null ? null : to.id());
        } else {
            Category selected = (Category) category.getSelectedItem();
            builder.categoryId(selected == null ? null : selected.id());
        }
        Transaction transaction = builder.build();
        if (existing == null) {
            app.transactions().add(transaction);
        } else {
            app.transactions().update(transaction);
        }
    }

    @Override
    public boolean supportsSaveAndNew() {
        return existing == null;
    }

    @Override
    public void reset() {
        amount.setText("");
        note.setText("");
        categoryChosenByUser = false;
        hint.setText(" ");
        time.setTime(LocalTime.now(app.clock()));
    }
}
