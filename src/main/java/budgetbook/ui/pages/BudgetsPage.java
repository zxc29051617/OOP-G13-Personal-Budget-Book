package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.BudgetLevel;
import budgetbook.model.BudgetStatus;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.CategoryTotal;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.ui.Page;
import budgetbook.ui.Prompts;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.PeriodPicker;
import budgetbook.ui.components.ProgressMeter;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.ThemedLabel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 每月總預算與分類預算：進度條、剩餘金額、超支提醒與月底預估。 */
public final class BudgetsPage extends Page {
    private final PeriodPicker picker;
    private final JPanel overall = new JPanel(new BorderLayout(0, 8));
    private final JPanel categories = new JPanel();
    private final ThemedLabel alerts = new ThemedLabel(" ", Fonts.bold(), Palette::warning);

    public BudgetsPage(UiContext ui) {
        super(ui);
        picker = new PeriodPicker(YearMonth.now(ui.app().clock()));
        picker.addListener(month -> refresh());
        add(header("每個月自動套用；使用率 80% 以上會提醒，超過 100% 會警告",
                picker, Widgets.primary("設定總預算", this::editOverall)), BorderLayout.NORTH);
        overall.setOpaque(false);
        categories.setOpaque(false);
        categories.setLayout(new BoxLayout(categories, BoxLayout.Y_AXIS));
        ScrollablePanel body = new ScrollablePanel(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        Card overallCard = Card.titled("每月總預算", overall);
        overallCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        alerts.setAlignmentX(Component.LEFT_ALIGNMENT);
        alerts.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 0));
        body.add(alerts);
        body.add(overallCard);
        body.add(Box.createVerticalStrut(14));
        Card categoryCard = Card.titled("分類預算", Widgets.muted("點「設定」輸入金額；清空代表取消"), categories);
        categoryCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(categoryCard);
        add(Widgets.scroll(body), BorderLayout.CENTER);
    }

    private void editOverall() {
        Money current = ui.app().budgets().overall().map(b -> b.monthlyLimit()).orElse(null);
        Prompts.amount(ui, "每月總預算", "每個月最多想花多少？（清空代表取消總預算）", current, true)
                .ifPresent(amount -> ui.run(() -> ui.app().budgets().setOverall(amount)));
    }

    private void editCategory(Category category) {
        Money current = ui.app().budgets().forCategory(category.id()).map(b -> b.monthlyLimit()).orElse(null);
        Prompts.amount(ui, "「" + category.name() + "」預算", "「" + category.name() + "」每個月的預算（清空代表取消）",
                current, true).ifPresent(amount -> ui.run(() -> ui.app().budgets().setForCategory(category.id(), amount)));
    }

    @Override
    public String id() {
        return "budgets";
    }

    @Override
    public String title() {
        return "預算";
    }

    @Override
    public String glyph() {
        return "算";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.BUDGETS, Topic.TRANSACTIONS, Topic.CATEGORIES);
    }

    @Override
    protected void reload() {
        YearMonth month = picker.value();
        List<BudgetStatus> statuses = ui.app().budgets().statuses(month);
        long over = statuses.stream().filter(s -> s.level() == BudgetLevel.OVER).count();
        long near = statuses.stream().filter(s -> s.level() == BudgetLevel.WARNING).count();
        alerts.setText(over + near == 0 ? (statuses.isEmpty() ? "還沒有設定預算。" : "所有預算都在 80% 以內，做得好！")
                : (over > 0 ? over + " 項已超支" : "") + (over > 0 && near > 0 ? "、" : "")
                + (near > 0 ? near + " 項接近上限" : "") + "。");
        alerts.setColor(over > 0 ? Palette::expense : near > 0 ? Palette::warning : Palette::accent);

        overall.removeAll();
        Optional<BudgetStatus> overallStatus = statuses.stream().filter(BudgetStatus::isOverall).findFirst();
        if (overallStatus.isPresent()) {
            BudgetStatus status = overallStatus.get();
            JPanel numbers = new JPanel(new BorderLayout());
            numbers.setOpaque(false);
            numbers.add(Widgets.value(status.spent().format() + " / " + status.limit().format(), 1.4f, Palette::text),
                    BorderLayout.WEST);
            numbers.add(Widgets.value(status.percent() + "%", 1.4f, () -> Palette.forLevel(status.level())),
                    BorderLayout.EAST);
            overall.add(numbers, BorderLayout.NORTH);
            ProgressMeter meter = new ProgressMeter(14);
            meter.setRatio(status.ratio());
            meter.setColor(() -> Palette.forLevel(status.level()));
            overall.add(meter, BorderLayout.CENTER);
            String remaining = status.remaining().isNegative() ? "已超出 " + status.remaining().abs().format()
                    : "還可以花 " + status.remaining().format();
            if (picker.isCurrent()) {
                remaining += "　·　依目前速度，月底預估支出 "
                        + ui.app().budgets().projectedSpending(month, status.spent()).format();
            }
            overall.add(Widgets.muted(remaining), BorderLayout.SOUTH);
        } else {
            overall.add(Widgets.muted("尚未設定每月總預算。按右上角「設定總預算」開始。"), BorderLayout.CENTER);
        }

        categories.removeAll();
        Map<Long, BudgetStatus> byCategory = new HashMap<>();
        statuses.stream().filter(s -> !s.isOverall() && s.category() != null)
                .forEach(s -> byCategory.put(s.category().id(), s));
        Map<Long, Money> spent = new HashMap<>();
        for (CategoryTotal total : ui.app().statistics().totalsByCategory(DateRange.ofMonth(month), CategoryKind.EXPENSE)) {
            spent.put(total.category().id(), total.total());
        }
        List<Category> ordered = new ArrayList<>(ui.app().categories().byKind(CategoryKind.EXPENSE));
        ordered.sort(Comparator.<Category>comparingInt(c -> byCategory.containsKey(c.id()) ? 0 : 1)
                .thenComparing(c -> byCategory.containsKey(c.id()) ? -byCategory.get(c.id()).ratio() : 0.0)
                .thenComparing(c -> spent.getOrDefault(c.id(), Money.ZERO), Comparator.reverseOrder()));
        for (Category category : ordered) {
            categories.add(categoryRow(category, byCategory.get(category.id()), spent.getOrDefault(category.id(), Money.ZERO)));
        }
    }

    private JComponent categoryRow(Category category, BudgetStatus status, Money spent) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JLabel name = new ThemedLabel(category.name(), Fonts.of(Font.BOLD, 1f), Palette::text);
        name.setIcon(BadgeIcon.of(category, 30));
        name.setIconTextGap(10);
        name.setPreferredSize(new Dimension(150, 34));
        row.add(name, BorderLayout.WEST);
        JPanel middle = new JPanel(new BorderLayout(0, 4));
        middle.setOpaque(false);
        if (status != null) {
            ProgressMeter meter = new ProgressMeter(10);
            meter.setRatio(status.ratio());
            meter.setColor(() -> Palette.forLevel(status.level()));
            middle.add(meter, BorderLayout.CENTER);
            String text = status.spent().format() + " / " + status.limit().format() + "（" + status.percent() + "%）· "
                    + (status.remaining().isNegative() ? "超出 " + status.remaining().abs().format()
                    : "剩 " + status.remaining().format());
            middle.add(new ThemedLabel(text, Fonts.small(), status.level() == BudgetLevel.OK ? Palette::muted
                    : () -> Palette.forLevel(status.level())), BorderLayout.SOUTH);
        } else {
            middle.add(Widgets.muted("未設定預算 · 本月已花 " + spent.format()), BorderLayout.CENTER);
        }
        row.add(middle, BorderLayout.CENTER);
        row.add(Widgets.button(status == null ? "設定" : "調整", () -> editCategory(category)), BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
