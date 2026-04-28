package ui;

import model.Account;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class WalletPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "帳戶", "類型", "初始金額", "目前餘額"}, 0);
    private final JTable table = new JTable(model);
    private final JTextField name = new JTextField(16);
    private final JTextField type = new JTextField(16);
    private final JTextField initial = new JTextField(16);

    public WalletPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(18, 18));
        setBackground(Ui.BACKGROUND);
        Ui.pad(this);
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(Ui.title("錢包頁面"), BorderLayout.NORTH);
        header.add(Ui.small("管理現金、銀行、信用卡與電子支付帳戶"), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (Account account : frame.getStore().getAccounts()) {
            model.addRow(new Object[]{
                    account.getId(),
                    account.getName(),
                    account.getType(),
                    Money.format(account.getInitialBalance()),
                    Money.format(frame.getStore().getAccountBalance(account.getId()))
            });
        }
    }

    private JPanel buildMain() {
        JPanel wrapper = new JPanel(new BorderLayout(14, 14));
        wrapper.setOpaque(false);

        JPanel list = Ui.card();
        list.setLayout(new BorderLayout(0, 10));
        Ui.styleTable(table);
        list.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton delete = Ui.quietButton("刪除選取帳戶");
        delete.addActionListener(e -> deleteSelected());
        list.add(delete, BorderLayout.SOUTH);
        wrapper.add(list, BorderLayout.CENTER);
        wrapper.add(buildForm(), BorderLayout.EAST);
        return wrapper;
    }

    private JPanel buildForm() {
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        card.add(Ui.sectionTitle("新增帳戶"), c);
        addRow(card, c, 1, "名稱", name);
        addRow(card, c, 2, "類型", type);
        addRow(card, c, 3, "初始金額", initial);
        JButton add = Ui.primaryButton("新增錢包");
        add.addActionListener(e -> addAccount());
        c.gridx = 1;
        c.gridy = 4;
        card.add(add, c);
        return card;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component input) {
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        panel.add(input, c);
    }

    private void addAccount() {
        try {
            String accountName = name.getText().trim();
            String accountType = type.getText().trim();
            if (accountName.isEmpty() || accountType.isEmpty()) {
                throw new IllegalArgumentException("名稱與類型不可空白。");
            }
            frame.getStore().addAccount(accountName, accountType, Double.parseDouble(initial.getText().trim()));
            name.setText("");
            type.setText("");
            initial.setText("");
            frame.persistAndRefresh();
        } catch (Exception ex) {
            Dialogs.showError(this, "新增帳戶失敗：" + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        try {
            int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
            frame.getStore().deleteAccount(id);
            frame.persistAndRefresh();
        } catch (Exception ex) {
            Dialogs.showError(this, ex.getMessage());
        }
    }
}
