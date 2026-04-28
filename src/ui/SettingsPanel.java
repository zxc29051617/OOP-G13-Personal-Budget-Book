package ui;

import model.BudgetSettings;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingsPanel extends RefreshablePanel {
    private final BudgetBookFrame frame;
    private final JTextField monthlyLimit = new JTextField(16);
    private final JLabel points = Ui.title("");
    private final JLabel tree = Ui.title("");

    public SettingsPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(18, 18));
        setBackground(Ui.BACKGROUND);
        Ui.pad(this);
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(Ui.title("設定與集點"), BorderLayout.NORTH);
        header.add(Ui.small("調整月支出限額，查看記帳點數與種樹等級"), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(buildSettings(), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        BudgetSettings settings = frame.getStore().getSettings();
        monthlyLimit.setText(String.valueOf((int) settings.getMonthlyLimit()));
        points.setText(settings.getPoints() + " 點");
        tree.setText(settings.getTreeLevel());
    }

    private JPanel buildSettings() {
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        card.add(new JLabel("每月支出限額"), c);
        c.gridx = 1;
        card.add(monthlyLimit, c);

        JButton save = Ui.primaryButton("儲存設定");
        save.addActionListener(e -> saveSettings());
        c.gridx = 1;
        c.gridy = 1;
        card.add(save, c);

        c.gridx = 0;
        c.gridy = 2;
        card.add(new JLabel("目前點數"), c);
        c.gridx = 1;
        card.add(points, c);

        c.gridx = 0;
        c.gridy = 3;
        card.add(new JLabel("種樹等級"), c);
        c.gridx = 1;
        card.add(tree, c);
        return card;
    }

    private void saveSettings() {
        try {
            double limit = Double.parseDouble(monthlyLimit.getText().trim());
            if (limit < 0) {
                throw new IllegalArgumentException("限額不可小於 0。");
            }
            frame.getStore().getSettings().setMonthlyLimit(limit);
            frame.persistAndRefresh();
            Dialogs.showInfo(this, "設定已儲存。");
        } catch (Exception ex) {
            Dialogs.showError(this, "儲存失敗：" + ex.getMessage());
        }
    }
}
