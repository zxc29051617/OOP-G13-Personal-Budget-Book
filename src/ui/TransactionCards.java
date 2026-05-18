package ui;

import model.Account;
import model.Transaction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TransactionCards {
    private TransactionCards() {
    }

    public static JPanel recent(BudgetBookFrame frame, int limit) {
        JPanel list = listPanel();
        List<Transaction> rows = frame.getStore().getTransactions();
        if (rows.isEmpty()) {
            list.add(emptyState("還沒有交易紀錄", "新增第一筆記帳後，這裡會顯示最近動態。"));
            return list;
        }
        for (int i = 0; i < Math.min(limit, rows.size()); i++) {
            list.add(card(frame, rows.get(i)));
            if (i < Math.min(limit, rows.size()) - 1) {
                list.add(Box.createVerticalStrut(8));
            }
        }
        return list;
    }

    public static JPanel all(BudgetBookFrame frame, java.util.function.IntConsumer onDelete) {
        JPanel list = listPanel();
        List<Transaction> rows = frame.getStore().getTransactions();
        if (rows.isEmpty()) {
            list.add(emptyState("目前沒有紀錄", "你可以從快速記帳新增收入或支出。"));
            return list;
        }
        for (int i = 0; i < rows.size(); i++) {
            Transaction transaction = rows.get(i);
            list.add(card(frame, transaction, onDelete));
            if (i < rows.size() - 1) {
                list.add(Box.createVerticalStrut(8));
            }
        }
        return list;
    }

    private static JPanel listPanel() {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        return list;
    }

    private static JPanel card(BudgetBookFrame frame, Transaction transaction) {
        return card(frame, transaction, null);
    }

    private static JPanel card(BudgetBookFrame frame, Transaction transaction, java.util.function.IntConsumer onDelete) {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        boolean income = transaction.getKind() == Transaction.Kind.INCOME;
        JLabel badge = badge(transaction.getCategory(), income ? Ui.MINT : Ui.CREAM, income ? Ui.GREEN_DARK : Ui.ORANGE);
        card.add(badge, BorderLayout.WEST);

        Account account = frame.getStore().findAccount(transaction.getAccountId());
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        JLabel title = new JLabel(transaction.getCategory());
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        title.setForeground(Ui.TEXT);
        String sub = transaction.getDate() + "  ·  " + (account == null ? "未知帳戶" : account.getName());
        if (transaction.getNote() != null && !transaction.getNote().isBlank()) {
            sub += "  ·  " + transaction.getNote();
        }
        JLabel subtitle = Ui.caption(sub);
        text.add(title);
        text.add(subtitle);
        card.add(text, BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(onDelete == null ? 1 : 2, 1, 0, 3));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(onDelete == null ? 84 : 96, 48));
        JLabel amount = new JLabel((income ? "+" : "-") + Money.format(transaction.getAmount()));
        amount.setHorizontalAlignment(JLabel.RIGHT);
        amount.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        amount.setForeground(income ? Ui.GREEN_DARK : Ui.RED);
        right.add(amount);
        if (onDelete != null) {
            javax.swing.JButton delete = Ui.dangerButton("刪除");
            delete.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            delete.addActionListener(e -> onDelete.accept(transaction.getId()));
            right.add(delete);
        }
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private static JLabel badge(String text, Color fill, Color foreground) {
        String value = text == null || text.isBlank() ? "?" : text.substring(0, 1);
        JLabel label = new JLabel(value, JLabel.CENTER);
        label.setOpaque(true);
        label.setBackground(fill);
        label.setForeground(foreground);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        label.setPreferredSize(new Dimension(38, 38));
        return label;
    }

    private static JPanel emptyState(String title, String message) {
        JPanel panel = Ui.tintedCard(Ui.SURFACE);
        panel.setLayout(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = Ui.sectionTitle(title);
        JLabel messageLabel = Ui.small(message);
        panel.add(titleLabel);
        panel.add(messageLabel);
        return panel;
    }
}
