package ui;

import persistence.BudgetBookStore;
import service.StatsService;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class BudgetBookFrame extends JFrame {
    private final BudgetBookStore store;
    private final StatsService stats;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, RefreshablePanel> panels = new LinkedHashMap<>();

    public BudgetBookFrame(BudgetBookStore store) {
        this.store = store;
        this.stats = new StatsService(store);
        setTitle("個人記帳本 - Group 13");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setLocationByPlatform(true);
        buildLayout();
        showPanel("home");
    }

    public BudgetBookStore getStore() {
        return store;
    }

    public StatsService getStats() {
        return stats;
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
        }
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(247, 248, 250));
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        addPanel("home", new HomePanel(this));
        addPanel("quick", new QuickEntryPanel(this));
        addPanel("analysis", new AnalysisPanel(this));
        addPanel("wallets", new WalletPanel(this));
        addPanel("history", new HistoryPanel(this));
        addPanel("settings", new SettingsPanel(this));
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Ui.SIDEBAR, 0, getHeight(), new Color(22, 86, 94)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(26, 16, 22, 16));

        JLabel title = new JLabel("<html><b>個人記帳本</b><br><span style='font-size:10px'>Group 13 Finance</span></html>");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 18, 0));
        sidebar.add(title, BorderLayout.NORTH);

        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 10));
        nav.setOpaque(false);
        nav.add(navButton("首頁", "home"));
        nav.add(navButton("快速記帳", "quick"));
        nav.add(navButton("分析", "analysis"));
        nav.add(navButton("錢包", "wallets"));
        nav.add(navButton("歷史", "history"));
        nav.add(navButton("設定", "settings"));
        sidebar.add(nav, BorderLayout.CENTER);
        return sidebar;
    }

    private JButton navButton(String text, String key) {
        JButton button = Ui.navButton(text);
        button.addActionListener(e -> showPanel(key));
        return button;
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
}
