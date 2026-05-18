package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.YearMonth;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AnalysisPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final CategoryChart chart;
    private final JLabel income = metricLabel(Ui.GREEN_DARK);
    private final JLabel expense = metricLabel(Ui.RED);
    private final JLabel balance = metricLabel(Ui.BLUE);
    private final JLabel summary = Ui.small("");

    public AnalysisPanel(BudgetBookFrame frame) {
        this.frame = frame;
        this.chart = new CategoryChart();
        setLayout(new BorderLayout());
        setBackground(Ui.SURFACE);

        JPanel body = Ui.page();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildHeader());
        body.add(Box.createVerticalStrut(16));
        body.add(buildSummaryCards());
        body.add(Box.createVerticalStrut(12));
        body.add(buildChartCard());
        body.add(Box.createVerticalStrut(12));
        body.add(buildInsightCard());
        add(Ui.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        YearMonth month = YearMonth.now();
        double monthIncome = frame.getStats().monthlyIncome(month);
        double monthExpense = frame.getStats().monthlyExpense(month);
        income.setText(Money.format(monthIncome));
        expense.setText(Money.format(monthExpense));
        balance.setText(Money.format(monthIncome - monthExpense));
        summary.setText(monthExpense > monthIncome
                ? "本月支出高於收入，建議優先檢查最大分類。"
                : "本月收支仍有餘裕，持續追蹤主要花費。");
        chart.setData(frame.getStats().monthlyExpenseByCategory(month));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(Ui.appName(), BorderLayout.NORTH);
        text.add(Ui.title("財務分析"), BorderLayout.CENTER);
        text.add(Ui.small("依分類查看本月支出與收支差額"), BorderLayout.SOUTH);
        header.add(text, BorderLayout.WEST);
        header.add(frame.settingsButton(), BorderLayout.EAST);
        header.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 86));
        return header;
    }

    private JPanel buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 8, 0));
        grid.setOpaque(false);
        grid.add(summaryCard("收入", income, Ui.MINT));
        grid.add(summaryCard("支出", expense, Ui.CREAM));
        grid.add(summaryCard("結餘", balance, Ui.CARD));
        return grid;
    }

    private JPanel summaryCard(String title, JLabel value, Color background) {
        JPanel card = Ui.tintedCard(background);
        card.setLayout(new BorderLayout(0, 8));
        card.add(Ui.caption(title), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildChartCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("支出分類"), BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInsightCard() {
        JPanel card = Ui.tintedCard(Ui.GREEN_SOFT);
        card.setLayout(new BorderLayout(0, 8));
        card.add(Ui.sectionTitle("本月提醒"), BorderLayout.NORTH);
        card.add(summary, BorderLayout.CENTER);
        return card;
    }

    private static JLabel metricLabel(Color color) {
        JLabel label = new JLabel();
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        label.setForeground(color);
        return label;
    }

    private static class CategoryChart extends JPanel {
        private Map<String, Double> data = Map.of();

        CategoryChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 310));
        }

        void setData(Map<String, Double> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

            if (data.isEmpty()) {
                g2.setColor(Ui.MUTED);
                g2.drawString("本月尚無支出資料", 10, 28);
                g2.dispose();
                return;
            }

            double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            int y = 12;
            int labelWidth = 78;
            int barMax = Math.max(120, getWidth() - labelWidth - 92);
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                int barWidth = (int) Math.max(8, Math.round(entry.getValue() / max * barMax));
                g2.setColor(Ui.TEXT);
                g2.drawString(entry.getKey(), 8, y + 17);
                g2.setColor(new Color(233, 234, 224));
                g2.fillRoundRect(labelWidth, y, barMax, 22, 16, 16);
                g2.setColor(Ui.GREEN);
                g2.fillRoundRect(labelWidth, y, barWidth, 22, 16, 16);
                g2.setColor(Ui.MUTED);
                g2.drawString(Money.format(entry.getValue()), labelWidth + barMax + 10, y + 17);
                y += 42;
            }
            g2.dispose();
        }
    }
}
