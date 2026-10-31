package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.SavingsGoal;
import budgetbook.ui.Page;
import budgetbook.ui.Prompts;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.ProgressMeter;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.ThemedLabel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.components.WrapLayout;
import budgetbook.ui.forms.GoalForm;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 儲蓄目標：進度、還差多少、每月需要存多少（類似 Firefly III 的 piggy bank）。 */
public final class GoalsPage extends Page {
    private final ScrollablePanel cards = new ScrollablePanel(new WrapLayout(FlowLayout.LEFT, 14, 14));
    private final JLabel total = Widgets.muted(" ");

    public GoalsPage(UiContext ui) {
        super(ui);
        add(header("為旅行、學費或緊急預備金存錢；存入的錢仍在原本的錢包，這裡只是標記用途",
                Widgets.primary("＋ 新增目標", () -> ui.open(new GoalForm(ui.app(), null)))), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setOpaque(false);
        body.add(total, BorderLayout.NORTH);
        body.add(Widgets.scroll(cards), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public String id() {
        return "goals";
    }

    @Override
    public String title() {
        return "目標";
    }

    @Override
    public String glyph() {
        return "存";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.GOALS);
    }

    @Override
    protected void reload() {
        cards.removeAll();
        List<SavingsGoal> goals = ui.app().goals().all();
        LocalDate today = LocalDate.now(ui.app().clock());
        for (SavingsGoal goal : goals) {
            cards.add(card(goal, today));
        }
        if (goals.isEmpty()) {
            cards.add(Widgets.empty("還沒有儲蓄目標。按右上角「新增目標」開始存第一筆旅行基金吧！"));
        }
        total.setText(goals.isEmpty() ? " " : "共 " + goals.size() + " 個目標，已存 " + ui.app().goals().totalSaved().format());
    }

    private JComponent more(SavingsGoal goal) {
        javax.swing.JButton more = Widgets.link("更多 ▾", () -> { });
        more.addActionListener(e -> {
            javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
            menu.add(WalletsPage.item("編輯", () -> ui.open(new GoalForm(ui.app(), goal))));
            menu.add(WalletsPage.item("刪除", () -> ui.run(() -> ui.app().goals().delete(goal.id()))));
            menu.show(more, 0, more.getHeight());
        });
        return more;
    }

    private JComponent card(SavingsGoal goal, LocalDate today) {
        Card card = new Card(new BorderLayout(0, 10));
        card.setPreferredSize(new Dimension(320, 210));
        JLabel name = new ThemedLabel(goal.name(), Fonts.of(Font.BOLD, 1.1f), Palette::text);
        name.setIcon(new BadgeIcon(goal.name().substring(0, goal.name().offsetByCodePoints(0, 1)),
                Palette.of(goal.color()), 32));
        name.setIconTextGap(10);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        top.add(Widgets.value(goal.percent() + "%", 1.3f, () -> Palette.of(goal.color())), BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        ProgressMeter meter = new ProgressMeter(12);
        meter.setRatio(goal.progress());
        meter.setColor(() -> Palette.of(goal.color()));
        String detail = goal.isReached() ? "已達成目標！"
                : "還差 " + goal.remaining().format() + goal.monthlyNeeded(today)
                .map(m -> "，每月約存 " + m.format()).orElse("");
        String deadline = goal.deadline() == null ? "沒有設定日期" : "目標日 " + goal.deadline()
                + (goal.deadline().isBefore(today) && !goal.isReached() ? "（已過期）" : "");
        card.add(Widgets.column(6, Widgets.value(goal.saved().format() + " / " + goal.target().format(), 1.05f,
                Palette::text), meter, Widgets.muted(detail), Widgets.muted(deadline)), BorderLayout.CENTER);
        card.add(Widgets.row(4,
                Widgets.button("存入", () -> Prompts.amount(ui, "存入「" + goal.name() + "」", "存入多少？", null, false)
                        .ifPresent(amount -> ui.run(() -> ui.app().goals().deposit(goal.id(), amount)))),
                Widgets.button("取出", () -> Prompts.amount(ui, "從「" + goal.name() + "」取出", "取出多少？", null, false)
                        .ifPresent(amount -> ui.run(() -> ui.app().goals().withdraw(goal.id(), amount)))),
                more(goal)), BorderLayout.SOUTH);
        return card;
    }
}
