package budgetbook.ui;

import budgetbook.model.MonthlyTree;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.ProgressMeter;
import budgetbook.ui.components.ThemedLabel;
import budgetbook.ui.components.TreeView;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/** 左側導覽列：程式名稱、各頁面按鈕，底部顯示本月小樹的進度。 */
final class Sidebar extends JPanel {
    private final Map<String, JToggleButton> buttons = new LinkedHashMap<>();
    private final ButtonGroup group = new ButtonGroup();
    private final TreeView tree = new TreeView(54);
    private final JLabel treeText = Widgets.muted("");
    private final ProgressMeter treeProgress = new ProgressMeter(6);

    Sidebar(List<Page> pages, Consumer<String> onSelect) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(196, 600));
        setBorder(BorderFactory.createEmptyBorder(18, 12, 14, 12));

        JLabel name = new ThemedLabel("個人記帳本", Fonts.of(Font.BOLD, 1.25f), Palette::text);
        name.setIcon(new BadgeIcon("帳", Palette.accent(), 30));
        name.setIconTextGap(10);
        name.setBorder(BorderFactory.createEmptyBorder(0, 6, 18, 0));
        add(name, BorderLayout.NORTH);

        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 3));
        nav.setOpaque(false);
        int index = 1;
        for (Page page : pages) {
            JToggleButton button = new JToggleButton(page.title());
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setIconTextGap(10);
            button.setFont(Fonts.of(Font.PLAIN, 1.0f));
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
            button.putClientProperty(FlatClientProperties.STYLE, "toolbar.selectedBackground:fade($Component.accentColor,22%);"
                    + "toolbar.hoverBackground:fade($Component.accentColor,10%);"
                    + "toolbar.pressedBackground:fade($Component.accentColor,30%);margin:7,10,7,10;arc:12");
            button.setToolTipText(index <= 10 ? page.title() + "（Ctrl+" + (index % 10) + "）" : page.title());
            button.addActionListener(e -> onSelect.accept(page.id()));
            group.add(button);
            buttons.put(page.id(), button);
            nav.add(button);
            index++;
        }
        JPanel navHolder = new JPanel(new BorderLayout());
        navHolder.setOpaque(false);
        navHolder.add(nav, BorderLayout.NORTH);
        add(navHolder, BorderLayout.CENTER);

        JPanel treeBox = new JPanel(new BorderLayout(8, 4));
        treeBox.setOpaque(false);
        treeBox.add(tree, BorderLayout.WEST);
        treeBox.add(Widgets.column(4, Widgets.muted("本月小樹"), treeText, treeProgress), BorderLayout.CENTER);
        treeBox.setBorder(BorderFactory.createEmptyBorder(10, 6, 0, 4));
        treeBox.setToolTipText("每記一筆帳就是澆一次水");
        add(treeBox, BorderLayout.SOUTH);
        updateIcons(pages);
    }

    void select(String id) {
        JToggleButton button = buttons.get(id);
        if (button != null) {
            button.setSelected(true);
        }
    }

    void updateTree(MonthlyTree month) {
        tree.setStage(month.stage());
        treeText.setText(month.records() + " / " + month.goal() + " 次 · " + month.stage().displayName());
        treeProgress.setRatio(month.progress());
    }

    void updateIcons(List<Page> pages) {
        for (Page page : pages) {
            buttons.get(page.id()).setIcon(new BadgeIcon(page.glyph(), Palette.accent(), 24));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Palette.sidebar());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
