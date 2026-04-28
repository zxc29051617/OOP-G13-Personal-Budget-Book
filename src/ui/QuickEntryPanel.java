package ui;

import model.Account;
import model.Transaction;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class QuickEntryPanel extends RefreshablePanel {
    private static final String[] EXPENSE_CATEGORIES = {"餐飲", "交通", "購物", "娛樂", "學習", "醫療", "其他"};
    private static final String[] INCOME_CATEGORIES = {"薪資", "獎學金", "零用錢", "投資", "其他"};

    private final BudgetBookFrame frame;
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"支出", "收入"});
    private final JComboBox<String> category = new JComboBox<>();
    private final JComboBox<Account> account = new JComboBox<>();
    private final JTextField date = new JTextField(LocalDate.now().toString(), 18);
    private final JTextField amount = new JTextField(18);
    private final JTextField note = new JTextField(18);

    public QuickEntryPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(18, 18));
        setBackground(Ui.BACKGROUND);
        Ui.pad(this);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        kind.addActionListener(e -> updateCategories());
        updateCategories();
    }

    @Override
    public void refresh() {
        account.removeAllItems();
        for (Account item : frame.getStore().getAccounts()) {
            account.addItem(item);
        }
    }

    private JPanel buildForm() {
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        addRow(card, c, 0, "類型", kind);
        addRow(card, c, 1, "日期 YYYY-MM-DD", date);
        addRow(card, c, 2, "分類", category);
        addRow(card, c, 3, "帳戶", account);
        addRow(card, c, 4, "金額", amount);
        addRow(card, c, 5, "備註", note);

        JButton save = Ui.primaryButton("新增紀錄");
        save.addActionListener(e -> submit());
        c.gridx = 1;
        c.gridy = 6;
        card.add(save, c);
        return card;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(Ui.title("快速記帳"), BorderLayout.NORTH);
        header.add(Ui.small("輸入金額、分類與帳戶後，系統會立即更新統計與點數"), BorderLayout.SOUTH);
        return header;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component input) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(input, c);
    }

    private void updateCategories() {
        category.removeAllItems();
        String[] values = kind.getSelectedIndex() == 0 ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        for (String value : values) {
            category.addItem(value);
        }
    }

    private void submit() {
        try {
            Account selectedAccount = (Account) account.getSelectedItem();
            if (selectedAccount == null) {
                throw new IllegalArgumentException("請先建立至少一個帳戶。");
            }
            double parsedAmount = Double.parseDouble(amount.getText().trim());
            if (parsedAmount <= 0) {
                throw new IllegalArgumentException("金額必須大於 0。");
            }
            Transaction.Kind txKind = kind.getSelectedIndex() == 0 ? Transaction.Kind.EXPENSE : Transaction.Kind.INCOME;
            frame.getStore().addTransaction(
                    LocalDate.parse(date.getText().trim()),
                    txKind,
                    String.valueOf(category.getSelectedItem()),
                    selectedAccount.getId(),
                    parsedAmount,
                    note.getText().trim());
            amount.setText("");
            note.setText("");
            date.setText(LocalDate.now().toString());
            frame.persistAndRefresh();
            Dialogs.showInfo(this, "記帳完成，已獲得 10 點。");
        } catch (Exception ex) {
            Dialogs.showError(this, "新增失敗：" + ex.getMessage());
        }
    }
}
