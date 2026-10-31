package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.Forest;
import budgetbook.model.MonthlyTree;
import budgetbook.model.TreeStage;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.ProgressMeter;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.StatTile;
import budgetbook.ui.components.ThemedLabel;
import budgetbook.ui.components.TreePainter;
import budgetbook.ui.components.TreeView;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.components.WrapLayout;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 我的森林（遊戲化）：每記一筆帳替本月小樹澆一次水，達到目標就長成大樹；
 * 過去每個月的樹排成一片森林，並記錄連續記帳天數。
 */
public final class ForestPage extends Page {
    private final TreeView bigTree = new TreeView(190);
    private final JLabel stage = new ThemedLabel("", Fonts.of(Font.BOLD, 1.5f), Palette::accent);
    private final JLabel watering = Widgets.label("");
    private final JLabel hint = Widgets.muted("");
    private final ProgressMeter meter = new ProgressMeter(12);
    private final StatTile completed = new StatTile("完成的樹", Palette::accent);
    private final StatTile streak = new StatTile("連續記帳", Palette::text);
    private final StatTile longest = new StatTile("最長紀錄", Palette::text);
    private final JPanel forest = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
    private final JLabel legend = Widgets.muted("");

    public ForestPage(UiContext ui) {
        super(ui);
        add(header("每記一筆帳就是替本月小樹澆一次水；定期交易自動入帳的不算"), BorderLayout.NORTH);
        ScrollablePanel body = new ScrollablePanel(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel current = new JPanel(new BorderLayout(24, 0));
        current.setOpaque(false);
        current.add(bigTree, BorderLayout.WEST);
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(completed);
        stats.add(streak);
        stats.add(longest);
        current.add(Widgets.column(10, Widgets.muted("本月小樹"), stage, watering, meter, hint,
                Box.createVerticalStrut(6), stats), BorderLayout.CENTER);
        Card currentCard = Card.titled("本月", current);
        currentCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(currentCard);
        body.add(Box.createVerticalStrut(14));

        forest.setOpaque(false);
        Card forestCard = Card.titled("我的森林", legend, forest);
        forestCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(forestCard);
        add(Widgets.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public String id() {
        return "forest";
    }

    @Override
    public String title() {
        return "森林";
    }

    @Override
    public String glyph() {
        return "樹";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.TRANSACTIONS, Topic.SETTINGS);
    }

    @Override
    protected void reload() {
        MonthlyTree tree = ui.app().trees().currentTree();
        Forest all = ui.app().trees().forest(24);
        bigTree.setStage(tree.stage());
        stage.setText(tree.stage().displayName() + "　" + tree.stage().statusText());
        watering.setText("本月已澆水 " + tree.records() + " / " + tree.goal() + " 次");
        meter.setRatio(tree.progress());
        hint.setText(tree.isCompleted() ? "本月的樹已經長成大樹了！繼續記帳可以維持連續紀錄。"
                : tree.records() == 0 ? "本月還沒有記帳，小樹枯萎了。記一筆帳就能讓它重新發芽。"
                : "再記 " + tree.remaining() + " 筆，本月就能完成一棵樹。");
        completed.setValue(all.completedCount() + " 棵");
        completed.setCaption("共 " + all.trees().size() + " 個月");
        streak.setValue(all.currentStreak() + " 天");
        streak.setCaption(all.currentStreak() > 0 ? "今天也要記帳喔" : "今天開始新的紀錄");
        longest.setValue(all.longestStreak() + " 天");
        longest.setCaption("總共記了 " + all.totalRecords() + " 筆");

        int goal = tree.goal();
        legend.setText("枯萎 0 · 種子 1+ · 發芽 " + (int) Math.ceil(goal * 0.25) + "+ · 幼苗 " + (int) Math.ceil(goal * 0.5)
                + "+ · 小樹 " + (int) Math.ceil(goal * 0.75) + "+ · 大樹 " + goal + "+");
        forest.removeAll();
        YearMonth now = YearMonth.now(ui.app().clock());
        for (MonthlyTree month : all.trees()) {
            forest.add(new ForestCell(month, month.month().equals(now)));
        }
    }

    /** 森林中的一格：一個月份的樹、月份與澆水次數。 */
    private static final class ForestCell extends JComponent {
        private final MonthlyTree tree;
        private final boolean current;

        ForestCell(MonthlyTree tree, boolean current) {
            this.tree = tree;
            this.current = current;
            setToolTipText(tree.month().getYear() + " 年 " + tree.month().getMonthValue() + " 月：澆水 " + tree.records()
                    + " 次，" + tree.stage().displayName());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(112, 142);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(tree.stage() == TreeStage.FULL_TREE ? Palette.accentSoft() : Palette.mix(Palette.card(), Palette.page(), 0.5));
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
            if (current) {
                g2.setColor(Palette.accent());
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            }
            TreePainter.paint(g2, 12, 6, getWidth() - 24, 88, tree.stage());
            g2.setFont(Fonts.of(Font.BOLD, 0.9f));
            g2.setColor(Palette.text());
            String label = current ? "本月" : tree.month().getYear() + "/" + tree.month().getMonthValue();
            center(g2, label, 112);
            g2.setFont(Fonts.small());
            g2.setColor(Palette.muted());
            center(g2, tree.records() + " 次 · " + tree.stage().displayName(), 130);
            g2.dispose();
        }

        private void center(Graphics2D g2, String text, int y) {
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
        }
    }
}
