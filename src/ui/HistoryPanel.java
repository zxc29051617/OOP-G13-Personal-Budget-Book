package ui;

import model.Account;
import model.Transaction;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class HistoryPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "日期", "類型", "分類", "帳戶", "金額", "備註"}, 0);
    private final JTable table = new JTable(model);

    public HistoryPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(18, 18));
        setBackground(Ui.BACKGROUND);
        Ui.pad(this);
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(Ui.title("歷史紀錄"), BorderLayout.NORTH);
        header.add(Ui.small("檢視所有收入與支出資料"), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));
        Ui.styleTable(table);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton delete = Ui.quietButton("刪除選取紀錄");
        delete.addActionListener(e -> deleteSelected());
        card.add(delete, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (Transaction t : frame.getStore().getTransactions()) {
            Account account = frame.getStore().findAccount(t.getAccountId());
            model.addRow(new Object[]{
                    t.getId(),
                    t.getDate(),
                    t.getKind() == Transaction.Kind.INCOME ? "收入" : "支出",
                    t.getCategory(),
                    account == null ? "已刪除帳戶" : account.getName(),
                    Money.format(t.getAmount()),
                    t.getNote()
            });
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
        frame.getStore().deleteTransaction(id);
        frame.persistAndRefresh();
    }
}
