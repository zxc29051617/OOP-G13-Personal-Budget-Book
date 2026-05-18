package ui;

import model.Account;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class WalletPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final JTextField name = new JTextField(16);
    private final JTextField type = new JTextField(16);
    private final JTextField initial = new JTextField(16);
    private final JLabel total = new JLabel();
    private final JPanel walletContainer = new JPanel();

    public WalletPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Ui.SURFACE);

        Ui.styleInput(name);
        Ui.styleInput(type);
        Ui.styleInput(initial);
        walletContainer.setOpaque(false);
        walletContainer.setLayout(new BoxLayout(walletContainer, BoxLayout.Y_AXIS));

        JPanel body = Ui.page();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildHeader());
        body.add(Box.createVerticalStrut(16));
        body.add(buildTotalCard());
        body.add(Box.createVerticalStrut(12));
        body.add(buildWalletList());
        body.add(Box.createVerticalStrut(12));
        body.add(buildForm());
        add(Ui.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        walletContainer.removeAll();
        double totalBalance = 0;
        java.util.List<Account> accounts = frame.getStore().getAccounts();
        if (accounts.isEmpty()) {
            walletContainer.add(emptyWallet());
        }
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            double balance = frame.getStore().getAccountBalance(account.getId());
            totalBalance += balance;
            walletContainer.add(walletCard(account, balance));
            if (i < accounts.size() - 1) {
                walletContainer.add(Box.createVerticalStrut(8));
            }
        }
        total.setText(Money.format(totalBalance));
        walletContainer.revalidate();
        walletContainer.repaint();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(Ui.appName(), BorderLayout.NORTH);
        text.add(Ui.title("錢包管理"), BorderLayout.CENTER);
        text.add(Ui.small("現金、銀行與數位支付集中管理"), BorderLayout.SOUTH);
        header.add(text, BorderLayout.WEST);
        header.add(frame.settingsButton(), BorderLayout.EAST);
        header.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 86));
        return header;
    }

    private JPanel buildTotalCard() {
        JPanel card = Ui.tintedCard(Ui.GREEN_SOFT);
        card.setLayout(new BorderLayout(0, 8));
        card.add(Ui.small("TOTAL BALANCE"), BorderLayout.NORTH);
        total.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        total.setForeground(Ui.GREEN_DARK);
        card.add(total, BorderLayout.CENTER);
        card.add(Ui.caption("所有錢包目前餘額加總"), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildWalletList() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("錢包列表"), BorderLayout.NORTH);
        card.add(walletContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel walletCard(Account account, double balance) {
        JPanel card = Ui.tintedCard(account.getType().contains("銀行") ? Ui.CREAM : Ui.MINT);
        card.setLayout(new BorderLayout(12, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JLabel badge = new JLabel(account.getName().isBlank() ? "錢" : account.getName().substring(0, 1), JLabel.CENTER);
        badge.setOpaque(true);
        badge.setBackground(Ui.CARD);
        badge.setForeground(Ui.GREEN_DARK);
        badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        badge.setPreferredSize(new Dimension(42, 42));
        card.add(badge, BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        text.add(Ui.sectionTitle(account.getName()));
        text.add(Ui.caption(account.getType()));
        text.add(Ui.caption("初始金額 " + Money.format(account.getInitialBalance())));
        card.add(text, BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 4));
        right.setOpaque(false);
        JLabel balanceLabel = new JLabel(Money.format(balance), JLabel.RIGHT);
        balanceLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        balanceLabel.setForeground(Ui.GREEN_DARK);
        JButton delete = Ui.dangerButton("刪除");
        delete.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        delete.addActionListener(e -> deleteAccount(account.getId()));
        right.add(balanceLabel);
        right.add(delete);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel emptyWallet() {
        JPanel panel = Ui.tintedCard(Ui.SURFACE);
        panel.setLayout(new GridLayout(2, 1, 0, 4));
        panel.add(Ui.sectionTitle("尚未建立錢包"));
        panel.add(Ui.small("新增現金、銀行或電子支付帳戶後會顯示在這裡。"));
        return panel;
    }

    private JPanel buildForm() {
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 0, 4, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        card.add(Ui.sectionTitle("新增錢包"), c);
        addRow(card, c, 1, "名稱", name);
        addRow(card, c, 2, "類型", type);
        addRow(card, c, 3, "初始金額", initial);

        JButton add = Ui.primaryButton("新增錢包");
        add.addActionListener(e -> addAccount());
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(add, c);
        return card;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component input) {
        c.gridx = 0;
        c.gridy = row * 2;
        c.gridwidth = 2;
        c.insets = new Insets(8, 0, 4, 0);
        panel.add(Ui.label(label), c);
        c.gridy = row * 2 + 1;
        c.insets = new Insets(0, 0, 8, 0);
        panel.add(input, c);
    }

    private void addAccount() {
        try {
            String accountName = name.getText().trim();
            String accountType = type.getText().trim();
            if (accountName.isEmpty() || accountType.isEmpty()) {
                throw new IllegalArgumentException("名稱與類型不能空白。");
            }
            frame.getStore().addAccount(accountName, accountType, Double.parseDouble(initial.getText().trim()));
            name.setText("");
            type.setText("");
            initial.setText("");
            frame.persistAndRefresh();
        } catch (Exception ex) {
            Dialogs.showError(this, "新增錢包失敗：" + ex.getMessage());
        }
    }

    private void deleteAccount(int id) {
        try {
            frame.getStore().deleteAccount(id);
            frame.persistAndRefresh();
        } catch (Exception ex) {
            Dialogs.showError(this, ex.getMessage());
        }
    }
}
