package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.YearMonth;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import service.StatsService;

public class SettingsPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final JTextField monthlyLimit = new JTextField(16);
    private final TreeGardenPanel treePanel = new TreeGardenPanel();
    private final JLabel waterText = new JLabel();
    private final JLabel completedText = new JLabel();
    private final JProgressBar treeProgress = new JProgressBar(0, 100);

    public SettingsPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Ui.SURFACE);
        Ui.styleInput(monthlyLimit);

        JPanel body = Ui.page();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildHeader());
        body.add(Box.createVerticalStrut(16));
        body.add(buildBudgetCard());
        body.add(Box.createVerticalStrut(12));
        body.add(buildTreeCard());
        add(Ui.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        YearMonth month = YearMonth.now();
        int waterCount = frame.getStats().monthlyWaterCount(month);
        String status = frame.getStats().monthlyTreeStatus(month);
        int percent = frame.getStats().monthlyTreeProgressPercent(month);

        monthlyLimit.setText(String.valueOf((int) frame.getStore().getSettings().getMonthlyLimit()));
        treePanel.setTreeState(waterCount, status);
        waterText.setText(waterCount + " / " + StatsService.MONTHLY_TREE_GOAL + " 次澆水");
        completedText.setText("已完成 " + frame.getStats().completedTreeCount() + " 棵樹");
        treeProgress.setValue(percent);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(Ui.appName(), BorderLayout.NORTH);
        header.add(Ui.title("設定"), BorderLayout.CENTER);
        header.add(Ui.small("調整預算與查看本月種樹進度"), BorderLayout.SOUTH);
        header.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 86));
        return header;
    }

    private JPanel buildBudgetCard() {
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 0, 4, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        card.add(Ui.sectionTitle("每月支出預算"), c);
        c.gridy = 1;
        card.add(Ui.small("首頁會依這個金額計算預算使用率"), c);
        c.gridy = 2;
        c.insets = new Insets(14, 0, 8, 0);
        card.add(monthlyLimit, c);

        JButton save = Ui.primaryButton("儲存設定");
        save.addActionListener(e -> saveSettings());
        c.gridy = 3;
        c.insets = new Insets(8, 0, 0, 0);
        card.add(save, c);
        return card;
    }

    private JPanel buildTreeCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("本月小樹"), BorderLayout.NORTH);
        card.add(treePanel, BorderLayout.CENTER);

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        waterText.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        waterText.setForeground(Ui.GREEN_DARK);
        details.add(waterText, c);
        c.gridy = 1;
        completedText.setForeground(Ui.MUTED);
        details.add(completedText, c);
        c.gridy = 2;
        c.insets = new Insets(10, 0, 0, 0);
        treeProgress.setStringPainted(false);
        treeProgress.setForeground(Ui.GREEN);
        treeProgress.setBackground(new java.awt.Color(231, 231, 221));
        treeProgress.setBorderPainted(false);
        details.add(treeProgress, c);
        c.gridy = 3;
        c.insets = new Insets(8, 0, 0, 0);
        details.add(Ui.caption("每新增一筆記帳就是澆水一次；本月達 20 次就完成一棵樹，沒有記錄會顯示枯萎。"), c);
        card.add(details, BorderLayout.SOUTH);
        return card;
    }

    private void saveSettings() {
        try {
            double limit = Double.parseDouble(monthlyLimit.getText().trim());
            if (limit < 0) {
                throw new IllegalArgumentException("預算不能小於 0。");
            }
            frame.getStore().getSettings().setMonthlyLimit(limit);
            frame.persistAndRefresh();
            Dialogs.showInfo(this, "設定已儲存。");
        } catch (Exception ex) {
            Dialogs.showError(this, "儲存設定失敗：" + ex.getMessage());
        }
    }
}
