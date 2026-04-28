package ui;

import model.Transaction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.time.YearMonth;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AnalysisPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final CategoryChart chart;
    private final JLabel summary = Ui.small("");

    public AnalysisPanel(BudgetBookFrame frame) {
        this.frame = frame;
        this.chart = new CategoryChart();
        setLayout(new BorderLayout(18, 18));
        setBackground(Ui.BACKGROUND);
        Ui.pad(this);
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(Ui.title("分析頁面"), BorderLayout.NORTH);
        header.add(Ui.small("本月支出依分類彙整"), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(summary, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        YearMonth month = YearMonth.now();
        double income = frame.getStats().monthlyIncome(month);
        double expense = frame.getStats().monthlyExpense(month);
        summary.setText("本月收入 " + Money.format(income) + "，本月支出 " + Money.format(expense)
                + "，結餘 " + Money.format(income - expense));
        chart.setData(frame.getStats().monthlyExpenseByCategory(month));
    }

    private static class CategoryChart extends JPanel {
        private Map<String, Double> data = Map.of();

        public CategoryChart() {
            setBackground(Color.WHITE);
        }

        public void setData(Map<String, Double> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) {
                g.setColor(Ui.MUTED);
                g.drawString("本月尚無支出資料。", 30, 40);
                return;
            }
            double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            int y = 30;
            int labelWidth = 90;
            int barMax = Math.max(240, getWidth() - 230);
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                int barWidth = (int) Math.round(entry.getValue() / max * barMax);
                g.setColor(Ui.TEXT);
                g.drawString(entry.getKey(), 20, y + 18);
                g.setColor(new Color(0, 136, 145));
                g.fillRect(20 + labelWidth, y, barWidth, 24);
                g.setColor(Ui.MUTED);
                g.drawString(Money.format(entry.getValue()), 30 + labelWidth + barWidth, y + 18);
                y += 42;
            }
        }
    }
}
