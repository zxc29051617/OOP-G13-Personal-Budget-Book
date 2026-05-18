package ui;

import model.BudgetSettings;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingsPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final JTextField monthlyLimit = new JTextField(16);
    private final JLabel points = new JLabel();
    private final JLabel level = new JLabel();

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
        body.add(buildPointsCard());
        add(Ui.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        BudgetSettings settings = frame.getStore().getSettings();
        monthlyLimit.setText(String.valueOf((int) settings.getMonthlyLimit()));
        points.setText(settings.getPoints() + " 點");
        level.setText(settings.getTreeLevel());
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(Ui.appName(), BorderLayout.NORTH);
        header.add(Ui.title("設定"), BorderLayout.CENTER);
        header.add(Ui.small("調整預算與查看記帳點數"), BorderLayout.SOUTH);
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

    private JPanel buildPointsCard() {
        JPanel card = Ui.tintedCard(Ui.GREEN_SOFT);
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("記帳成就"), BorderLayout.NORTH);
        JPanel values = new JPanel(new GridBagLayout());
        values.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        points.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        points.setForeground(Ui.GREEN_DARK);
        values.add(points, c);
        c.gridy = 1;
        level.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        level.setForeground(Ui.TEXT);
        values.add(level, c);
        card.add(values, BorderLayout.CENTER);
        card.add(Ui.caption("每新增一筆交易會增加 10 點"), BorderLayout.SOUTH);
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
