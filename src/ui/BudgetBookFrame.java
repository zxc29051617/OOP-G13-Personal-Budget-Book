package ui;

import persistence.BudgetBookStore;
import service.StatsService;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class BudgetBookFrame extends JFrame {
    private final BudgetBookStore store;
    private final StatsService stats;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, RefreshablePanel> panels = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public BudgetBookFrame(BudgetBookStore store) {
        this.store = store;
        this.stats = new StatsService(store);
        setTitle("個人記帳本 - Group 13");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(420, 720));
        setPreferredSize(new Dimension(460, 780));
        setLocationByPlatform(true);
        buildLayout();
        pack();
        showPanel("home");
    }

    public BudgetBookStore getStore() {
        return store;
    }

    public StatsService getStats() {
        return stats;
    }

    public JButton settingsButton() {
        JButton button = Ui.quietButton("設定");
        button.addActionListener(e -> showPanel("settings"));
        return button;
    }

    public void persistAndRefresh() {
        try {
            store.save();
            refreshAll();
        } catch (IOException e) {
            Dialogs.showError(this, "儲存資料失敗：" + e.getMessage());
        }
    }

    public void showPanel(String key) {
        RefreshablePanel panel = panels.get(key);
        if (panel != null) {
            panel.refresh();
            cards.show(content, key);
            updateNavSelection(key);
        }
    }

    private void buildLayout() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Ui.BACKGROUND);
        setContentPane(root);

        JPanel appShell = new JPanel(new BorderLayout());
        appShell.setBackground(Ui.SURFACE);
        appShell.setPreferredSize(new Dimension(420, 720));
        appShell.setMinimumSize(new Dimension(390, 660));
        appShell.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, new Color(230, 230, 220)));

        content.setOpaque(false);
        appShell.add(content, BorderLayout.CENTER);
        appShell.add(buildBottomNav(), BorderLayout.SOUTH);

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        root.add(appShell, c);

        addPanel("home", new HomePanel(this));
        addPanel("quick", new QuickEntryPanel(this));
        addPanel("analysis", new AnalysisPanel(this));
        addPanel("wallets", new WalletPanel(this));
        addPanel("history", new HistoryPanel(this));
        addPanel("settings", new SettingsPanel(this));
    }

    private JPanel buildBottomNav() {
        JPanel nav = new JPanel(new GridLayout(1, 5, 4, 0));
        nav.setBackground(Ui.CARD);
        nav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.LINE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        addNavButton(nav, "home", "首頁");
        addNavButton(nav, "quick", "記帳");
        addNavButton(nav, "analysis", "分析");
        addNavButton(nav, "wallets", "錢包");
        addNavButton(nav, "history", "紀錄");
        return nav;
    }

    private void addNavButton(JPanel nav, String key, String text) {
        JButton button = Ui.navButton(text);
        button.addActionListener(e -> showPanel(key));
        navButtons.put(key, button);
        nav.add(button);
    }

    private void addPanel(String key, RefreshablePanel panel) {
        panels.put(key, panel);
        content.add(panel, key);
    }

    private void refreshAll() {
        for (RefreshablePanel panel : panels.values()) {
            panel.refresh();
        }
    }

    private void updateNavSelection(String selectedKey) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            Ui.setNavSelected(entry.getValue(), entry.getKey().equals(selectedKey));
        }
    }
}
