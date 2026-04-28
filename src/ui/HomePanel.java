package ui;

import model.Account;
import model.Transaction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.YearMonth;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class HomePanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final JLabel balance = metricLabel();
    private final JLabel income = metricLabel();
    private final JLabel expense = metricLabel();
    private final JLabel budget = metricLabel();
    private final DefaultTableModel recentModel = new DefaultTableModel(
            new String[]{"日期", "類型", "分類", "帳戶", "金額", "備註"}, 0);

    public HomePanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(18, 18));
        setBackground(Ui.BACKGROUND);
        Ui.pad(this);

        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(18, 18));
        center.setOpaque(false);
        center.add(buildMetrics(), BorderLayout.NORTH);
        center.add(buildRecent(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        YearMonth month = YearMonth.now();
        double monthExpense = frame.getStats().monthlyExpense(month);
        balance.setText(Money.format(frame.getStats().totalBalance()));
        income.setText(Money.format(frame.getStats().monthlyIncome(month)));
        expense.setText(Money.format(monthExpense));
        double limit = frame.getStore().getSettings().getMonthlyLimit();
        budget.setText(limit <= 0 ? "未設定" : String.format("%.0f%%", monthExpense / limit * 100));

        recentModel.setRowCount(0);
        List<Transaction> rows = frame.getStore().getTransactions();
        for (int i = 0; i < Math.min(8, rows.size()); i++) {
            Transaction t = rows.get(i);
            Account account = frame.getStore().findAccount(t.getAccountId());
            recentModel.addRow(new Object[]{
                    t.getDate(),
                    t.getKind() == Transaction.Kind.INCOME ? "收入" : "支出",
                    t.getCategory(),
                    account == null ? "已刪除帳戶" : account.getName(),
                    Money.format(t.getAmount()),
                    t.getNote()
            });
        }
    }

    private JPanel buildMetrics() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);
        grid.add(metricCard("總結餘", balance, Ui.BLUE));
        grid.add(metricCard("本月收入", income, Ui.GREEN));
        grid.add(metricCard("本月支出", expense, Ui.RED));
        grid.add(metricCard("預算使用率", budget, new Color(145, 99, 36)));
        return grid;
    }

    private JPanel metricCard(String title, JLabel value, Color color) {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));
        card.add(Ui.small(title), BorderLayout.NORTH);
        value.setForeground(color);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel(new BorderLayout(0, 6));
        copy.setOpaque(false);
        copy.add(Ui.title("首頁總覽"), BorderLayout.NORTH);
        copy.add(Ui.small("追蹤多帳戶收支、預算狀態與最近紀錄"), BorderLayout.SOUTH);
        header.add(copy, BorderLayout.WEST);
        JButton quick = Ui.primaryButton("新增一筆");
        quick.addActionListener(e -> frame.showPanel("quick"));
        header.add(quick, BorderLayout.EAST);
        return header;
    }

    private JPanel buildRecent() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("最近紀錄"), BorderLayout.NORTH);
        JTable table = new JTable(recentModel);
        Ui.styleTable(table);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private static JLabel metricLabel() {
        JLabel label = new JLabel();
        label.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 24));
        return label;
    }
}
