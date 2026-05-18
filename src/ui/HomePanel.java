package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.YearMonth;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class HomePanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final JLabel balance = metricLabel(30);
    private final JLabel income = metricLabel(18);
    private final JLabel expense = metricLabel(18);
    private final JLabel budget = metricLabel(18);
    private final JProgressBar budgetProgress = new JProgressBar(0, 100);
    private final JPanel recentContainer = new JPanel(new BorderLayout());

    public HomePanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Ui.SURFACE);

        recentContainer.setOpaque(false);

        JPanel body = Ui.page();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildHeader());
        body.add(Box.createVerticalStrut(16));
        body.add(buildBalanceCard());
        body.add(Box.createVerticalStrut(12));
        body.add(buildMetrics());
        body.add(Box.createVerticalStrut(12));
        body.add(buildBudgetCard());
        body.add(Box.createVerticalStrut(12));
        body.add(buildRecent());

        add(Ui.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        YearMonth month = YearMonth.now();
        double monthIncome = frame.getStats().monthlyIncome(month);
        double monthExpense = frame.getStats().monthlyExpense(month);
        double limit = frame.getStore().getSettings().getMonthlyLimit();
        int percent = limit <= 0 ? 0 : (int) Math.min(100, Math.round(monthExpense / limit * 100));

        balance.setText(Money.format(frame.getStats().totalBalance()));
        income.setText(Money.format(monthIncome));
        expense.setText(Money.format(monthExpense));
        budget.setText(limit <= 0 ? "未設定" : percent + "%");
        budgetProgress.setValue(percent);

        recentContainer.removeAll();
        recentContainer.add(TransactionCards.recent(frame, 5), BorderLayout.CENTER);
        recentContainer.revalidate();
        recentContainer.repaint();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(Ui.appName(), BorderLayout.NORTH);
        text.add(Ui.title("首頁"), BorderLayout.CENTER);
        text.add(Ui.small("掌握結餘、預算與最近交易"), BorderLayout.SOUTH);
        header.add(text, BorderLayout.WEST);

        JButton quick = Ui.primaryButton("快速記帳");
        quick.addActionListener(e -> frame.showPanel("quick"));
        header.add(quick, BorderLayout.EAST);
        header.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 86));
        return header;
    }

    private JPanel buildBalanceCard() {
        JPanel card = Ui.tintedCard(Ui.GREEN_SOFT);
        card.setLayout(new BorderLayout(0, 8));
        card.add(Ui.small("TOTAL BALANCE"), BorderLayout.NORTH);
        balance.setForeground(Ui.GREEN_DARK);
        card.add(balance, BorderLayout.CENTER);
        card.add(Ui.caption("所有錢包加總後的目前餘額"), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildMetrics() {
        JPanel grid = new JPanel(new GridLayout(1, 2, 10, 0));
        grid.setOpaque(false);
        grid.add(metricCard("本月收入", income, Ui.GREEN_DARK, Ui.MINT));
        grid.add(metricCard("本月支出", expense, Ui.RED, Ui.CREAM));
        return grid;
    }

    private JPanel metricCard(String title, JLabel value, Color valueColor, Color background) {
        JPanel card = Ui.tintedCard(background);
        card.setLayout(new BorderLayout(0, 8));
        card.add(Ui.small(title), BorderLayout.NORTH);
        value.setForeground(valueColor);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBudgetCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.sectionTitle("預算使用率"), BorderLayout.WEST);
        budget.setHorizontalAlignment(JLabel.RIGHT);
        budget.setForeground(Ui.ORANGE);
        top.add(budget, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        budgetProgress.setStringPainted(false);
        budgetProgress.setForeground(Ui.GREEN);
        budgetProgress.setBackground(new Color(231, 231, 221));
        budgetProgress.setBorderPainted(false);
        card.add(budgetProgress, BorderLayout.CENTER);
        card.add(Ui.caption("依照設定頁的每月支出預算計算"), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildRecent() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("最近交易"), BorderLayout.NORTH);
        card.add(recentContainer, BorderLayout.CENTER);
        return card;
    }

    private static JLabel metricLabel(int size) {
        JLabel label = new JLabel();
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        label.setForeground(Ui.TEXT);
        return label;
    }
}
