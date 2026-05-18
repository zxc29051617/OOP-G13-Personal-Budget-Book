package ui;

import model.Account;
import model.Transaction;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class QuickEntryPanel extends RefreshablePanel {
    private static final String[] EXPENSE_CATEGORIES = {"餐飲", "交通", "購物", "娛樂", "生活", "醫療", "其他"};
    private static final String[] INCOME_CATEGORIES = {"薪資", "獎金", "投資", "轉入", "其他"};

    private final BudgetBookFrame frame;
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"支出", "收入"});
    private final JComboBox<String> category = new JComboBox<>();
    private final JComboBox<Account> account = new JComboBox<>();
    private final JTextField date = new JTextField(LocalDate.now().toString(), 18);
    private final JTextField amount = new JTextField(18);
    private final JTextField note = new JTextField(18);

    public QuickEntryPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Ui.SURFACE);

        Ui.styleCombo(kind);
        Ui.styleCombo(category);
        Ui.styleCombo(account);
        Ui.styleInput(date);
        Ui.styleInput(amount);
        Ui.styleInput(note);

        JPanel body = Ui.page();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildHeader());
        body.add(Box.createVerticalStrut(16));
        body.add(buildAmountPreview());
        body.add(Box.createVerticalStrut(12));
        body.add(buildForm());
        add(Ui.scroll(body), BorderLayout.CENTER);

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

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(Ui.appName(), BorderLayout.NORTH);
        header.add(Ui.title("快速記帳"), BorderLayout.CENTER);
        header.add(Ui.small("輸入金額、分類與備註，一次完成記錄"), BorderLayout.SOUTH);
        header.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 86));
        return header;
    }

    private JPanel buildAmountPreview() {
        JPanel card = Ui.tintedCard(Ui.CREAM);
        card.setLayout(new BorderLayout(0, 6));
        card.add(Ui.small("STEP 1  金額"), BorderLayout.NORTH);
        JLabel prompt = new JLabel("今天花了或收入多少？");
        prompt.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 24));
        prompt.setForeground(Ui.TEXT);
        card.add(prompt, BorderLayout.CENTER);
        card.add(Ui.caption("日期格式請使用 YYYY-MM-DD"), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildForm() {
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 0, 8, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        addRow(card, c, 0, "類型", kind);
        addRow(card, c, 1, "日期", date);
        addRow(card, c, 2, "分類", category);
        addRow(card, c, 3, "錢包", account);
        addRow(card, c, 4, "金額", amount);
        addRow(card, c, 5, "備註", note);

        JButton save = Ui.primaryButton("儲存這筆記帳");
        save.addActionListener(e -> submit());
        c.gridx = 0;
        c.gridy = 12;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(save, c);
        return card;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component input) {
        c.gridx = 0;
        c.gridy = row * 2;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(7, 0, 4, 0);
        panel.add(Ui.label(label), c);
        c.gridx = 0;
        c.gridy = row * 2 + 1;
        c.insets = new Insets(0, 0, 8, 0);
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
                throw new IllegalArgumentException("請先新增或選擇一個錢包。");
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
            Dialogs.showInfo(this, "已儲存，點數 +10。");
            frame.showPanel("home");
        } catch (Exception ex) {
            Dialogs.showError(this, "新增記帳失敗：" + ex.getMessage());
        }
    }
}
