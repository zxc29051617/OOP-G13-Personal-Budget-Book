package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.repository.StorageInfo;
import budgetbook.ui.DataActions;
import budgetbook.ui.Page;
import budgetbook.ui.Shortcuts;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.FormPanel;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.Tables;
import budgetbook.ui.components.Widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** 設定：外觀、種樹目標、資料庫與匯入匯出、鍵盤快捷鍵與關於。 */
public final class SettingsPage extends Page {
    private final JCheckBox dark = new JCheckBox("深色模式（Ctrl+Shift+D）");
    private final JSpinner treeGoal = new JSpinner(new SpinnerNumberModel(20, 5, 100, 1));
    private final JLabel backend = Widgets.label("");
    private final JLabel location = Widgets.label("");
    private final JLabel schema = Widgets.label("");
    private final JLabel counts = Widgets.label("");
    private boolean updating;

    public SettingsPage(UiContext ui, Consumer<Boolean> themeSwitcher) {
        super(ui);
        add(header("外觀、種樹目標與資料管理"), BorderLayout.NORTH);
        ScrollablePanel body = new ScrollablePanel(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        dark.addActionListener(e -> {
            if (!updating) {
                themeSwitcher.accept(dark.isSelected());
            }
        });
        treeGoal.addChangeListener(e -> {
            if (!updating) {
                ui.run(() -> ui.app().settings().setTreeGoal((Integer) treeGoal.getValue()));
            }
        });
        FormPanel appearance = new FormPanel();
        appearance.add("主題", dark);
        appearance.add("每月澆水目標", treeGoal);
        appearance.addHint("原版規則是一個月記 20 筆完成一棵樹，可以依自己的習慣調整。");

        FormPanel data = new FormPanel();
        data.add("資料庫", backend);
        data.add("位置", location);
        data.add("Schema 版本", schema);
        data.add("資料量", counts);
        data.addWide(Widgets.row(8,
                Widgets.button("匯入交易 CSV", () -> DataActions.importTransactions(ui)),
                Widgets.button("匯出全部交易 CSV", () -> DataActions.exportTransactions(ui,
                        ui.app().transactions().search(budgetbook.model.TransactionQuery.all()))),
                Widgets.button("載入示範資料…", () -> DataActions.loadDemoData(ui)),
                Widgets.button("清除所有資料…", () -> DataActions.resetData(ui))));
        data.addHint("想改用 MySQL 資料庫，請參考使用說明中的「資料庫設定」。");

        DefaultTableModel shortcuts = new DefaultTableModel(new Object[]{"按鍵", "功能"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        Shortcuts.ALL.forEach(s -> shortcuts.addRow(new Object[]{s.keys(), s.description()}));
        JTable table = new JTable(shortcuts);
        Tables.style(table);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.setOpaque(false);
        tableBox.add(table.getTableHeader(), BorderLayout.NORTH);
        tableBox.add(table, BorderLayout.CENTER);

        JPanel top = new JPanel(new GridLayout(1, 2, 14, 0));
        top.setOpaque(false);
        top.add(Card.titled("外觀與遊戲", Widgets.top(appearance)));
        top.add(Card.titled("資料", Widgets.top(data)));
        body.add(left(top));
        body.add(Box.createVerticalStrut(14));
        JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0));
        bottom.setOpaque(false);
        bottom.add(Card.titled("鍵盤快捷鍵", tableBox));
        bottom.add(Card.titled("關於", Widgets.column(6,
                Widgets.heading("個人記帳本 Personal Budget Book 2.0"),
                Widgets.label("臺大 114-2 物件導向程式語言（黃乾綱老師）第 13 組期末專題。"),
                Widgets.label("原版為課堂繳交的 Swing + MySQL 記帳程式；這一版延伸加入"),
                Widgets.label("內建 SQLite、預算、定期交易、報表圖表、復原與重做等功能。"),
                Widgets.muted("使用 FlatLaf、XChart、SQLite JDBC（皆為 Apache License 2.0）。"))));
        body.add(left(bottom));
        add(Widgets.scroll(body), BorderLayout.CENTER);
    }

    private static JComponent left(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    @Override
    public String id() {
        return "settings";
    }

    @Override
    public String title() {
        return "設定";
    }

    @Override
    public String glyph() {
        return "設";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.allOf(Topic.class);
    }

    @Override
    protected void reload() {
        updating = true;
        dark.setSelected(ui.app().settings().isDarkTheme());
        treeGoal.setValue(ui.app().settings().treeGoal());
        updating = false;
        StorageInfo info = ui.app().storage().info();
        backend.setText(info.backend());
        location.setText(info.location());
        location.setToolTipText(info.location());
        schema.setText("v" + info.schemaVersion());
        counts.setText(ui.app().transactions().count() + " 筆交易、" + ui.app().wallets().all().size() + " 個錢包、"
                + ui.app().categories().all().size() + " 個分類");
    }

    /** 讓其他地方切換主題後同步核取方塊。 */
    public void syncTheme() {
        updating = true;
        dark.setSelected(ui.app().settings().isDarkTheme());
        updating = false;
    }
}
