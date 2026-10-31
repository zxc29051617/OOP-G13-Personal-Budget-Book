package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.CategoryTotal;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.forms.CategoryForm;
import budgetbook.ui.theme.Fonts;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** 自訂分類：支出與收入分類的名稱、顏色、圖示，可以合併與刪除。 */
public final class CategoriesPage extends Page {
    private final Map<CategoryKind, DefaultListModel<Category>> models = new HashMap<>();
    private final Map<CategoryKind, JList<Category>> lists = new HashMap<>();
    private Map<Long, CategoryTotal> monthTotals = Map.of();

    public CategoriesPage(UiContext ui) {
        super(ui);
        add(header("自訂分類的名稱、顏色與圖示；雙擊可以編輯"), BorderLayout.NORTH);
        JPanel columns = new JPanel(new GridLayout(1, 2, 14, 0));
        columns.setOpaque(false);
        columns.add(column(CategoryKind.EXPENSE));
        columns.add(column(CategoryKind.INCOME));
        add(columns, BorderLayout.CENTER);
    }

    private Card column(CategoryKind kind) {
        DefaultListModel<Category> model = new DefaultListModel<>();
        JList<Category> list = new JList<>(model);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected,
                                                          boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(l, value, index, selected, focused);
                Category category = (Category) value;
                CategoryTotal total = monthTotals.get(category.id());
                label.setIcon(BadgeIcon.of(category, 30));
                label.setIconTextGap(12);
                label.setFont(Fonts.regular());
                label.setText("<html><b>" + escape(category.name()) + "</b><br><span style='font-size:90%'>本月 "
                        + (total == null ? Money.ZERO.format() + " · 0 筆" : total.total().format() + " · " + total.count() + " 筆")
                        + "</span></html>");
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return label;
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    ui.open(new CategoryForm(ui.app(), list.getSelectedValue(), kind));
                }
            }
        });
        models.put(kind, model);
        lists.put(kind, list);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel buttons = Widgets.row(6,
                Widgets.button("＋ 新增", () -> ui.open(new CategoryForm(ui.app(), null, kind))),
                Widgets.button("編輯", () -> withSelected(kind, c -> ui.open(new CategoryForm(ui.app(), c, kind)))),
                Widgets.button("看交易", () -> withSelected(kind, c -> {
                    ui.navigate("transactions");
                    ui.app().bus().publish(new FilterRequest(c, null));
                })),
                Widgets.button("合併到…", () -> withSelected(kind, this::merge)),
                Widgets.button("刪除", () -> withSelected(kind, c -> ui.run(() -> ui.app().categories().delete(c.id())))));
        Card card = Card.titled(kind.displayName() + "分類", scroll);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private void withSelected(CategoryKind kind, java.util.function.Consumer<Category> action) {
        Category selected = lists.get(kind).getSelectedValue();
        if (selected == null) {
            ui.showInfo("分類", "請先在清單中選擇一個分類。");
            return;
        }
        action.accept(selected);
    }

    private void merge(Category source) {
        List<Category> targets = ui.app().categories().byKind(source.kind()).stream()
                .filter(c -> c.id() != source.id()).toList();
        if (targets.isEmpty()) {
            ui.showInfo("合併分類", "沒有其他" + source.kind().displayName() + "分類可以合併。");
            return;
        }
        JComboBox<Category> target = new JComboBox<>(targets.toArray(Category[]::new));
        int choice = JOptionPane.showConfirmDialog(ui.window(), Widgets.column(8,
                Widgets.label("把「" + source.name() + "」的所有交易移到："), target,
                Widgets.muted("移動後會刪除「" + source.name() + "」，可以按 Ctrl+Z 復原。")),
                "合併分類", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION && target.getSelectedItem() instanceof Category chosen) {
            ui.run(() -> {
                int moved = ui.app().categories().mergeInto(source.id(), chosen.id());
                ui.status("已把 " + moved + " 筆交易從「" + source.name() + "」移到「" + chosen.name() + "」");
            });
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public String id() {
        return "categories";
    }

    @Override
    public String title() {
        return "分類";
    }

    @Override
    public String glyph() {
        return "類";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.CATEGORIES, Topic.TRANSACTIONS);
    }

    @Override
    protected void reload() {
        DateRange month = DateRange.ofMonth(YearMonth.now(ui.app().clock()));
        Map<Long, CategoryTotal> totals = new HashMap<>();
        for (CategoryKind kind : CategoryKind.values()) {
            ui.app().statistics().totalsByCategory(month, kind).forEach(t -> totals.put(t.category().id(), t));
        }
        monthTotals = totals;
        for (CategoryKind kind : CategoryKind.values()) {
            DefaultListModel<Category> model = models.get(kind);
            model.clear();
            ui.app().categories().byKind(kind).forEach(model::addElement);
        }
    }
}
