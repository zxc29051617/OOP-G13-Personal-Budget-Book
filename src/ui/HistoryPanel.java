package ui;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class HistoryPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final JPanel transactionContainer = new JPanel(new BorderLayout());

    public HistoryPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Ui.SURFACE);
        transactionContainer.setOpaque(false);

        JPanel body = Ui.page();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildHeader());
        body.add(Box.createVerticalStrut(16));
        body.add(buildHistoryCard());
        add(Ui.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        transactionContainer.removeAll();
        transactionContainer.add(TransactionCards.all(frame, this::deleteTransaction), BorderLayout.CENTER);
        transactionContainer.revalidate();
        transactionContainer.repaint();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(Ui.appName(), BorderLayout.NORTH);
        header.add(Ui.title("交易紀錄"), BorderLayout.CENTER);
        header.add(Ui.small("查看並管理所有收入與支出"), BorderLayout.SOUTH);
        header.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 86));
        return header;
    }

    private JPanel buildHistoryCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("全部紀錄"), BorderLayout.NORTH);
        card.add(transactionContainer, BorderLayout.CENTER);
        return card;
    }

    private void deleteTransaction(int id) {
        frame.getStore().deleteTransaction(id);
        frame.persistAndRefresh();
    }
}
