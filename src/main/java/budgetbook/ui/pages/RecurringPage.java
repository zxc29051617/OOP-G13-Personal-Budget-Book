package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.Category;
import budgetbook.model.RecurringRule;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.service.RecurringService;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.Tables;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.forms.RecurringForm;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/** 定期交易：房租、薪資、訂閱等固定收支；到期自動入帳，也可以提前入帳或略過一次。 */
public final class RecurringPage extends Page {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final JPanel upcoming = new JPanel();
    private final JButton postDue = Widgets.button("入帳到期項目", this::postDue);

    public RecurringPage(UiContext ui) {
        super(ui);
        add(header("到期的定期交易會在啟動時自動入帳，整批可以 Ctrl+Z 復原",
                postDue, Widgets.primary("＋ 新增定期交易", () -> ui.open(new RecurringForm(ui.app(), null)))),
                BorderLayout.NORTH);
        Tables.style(table);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(1).setCellRenderer(Tables.textRenderer(row -> Palette.forType(model.rows.get(row).type())));
        table.getColumnModel().getColumn(2).setCellRenderer(Tables.moneyRenderer(row -> Palette.forType(model.rows.get(row).type()), false));
        int[] widths = {120, 56, 96, 150, 110, 80, 104, 64};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    withSelected(rule -> ui.open(new RecurringForm(ui.app(), rule)));
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        Card tableCard = new Card(new BorderLayout(0, 10));
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(Widgets.row(6,
                Widgets.button("編輯", () -> withSelected(rule -> ui.open(new RecurringForm(ui.app(), rule)))),
                Widgets.button("立即入帳下一次", () -> withSelected(rule -> ui.run(() -> ui.app().recurring().postNext(rule.id())))),
                Widgets.button("略過一次", () -> withSelected(rule -> ui.run(() -> ui.app().recurring().skipNext(rule.id())))),
                Widgets.button("暫停／啟用", () -> withSelected(rule -> ui.run(
                        () -> ui.app().recurring().setActive(rule.id(), !rule.active())))),
                Widgets.button("刪除", () -> withSelected(rule -> ui.run(() -> ui.app().recurring().delete(rule.id()))))),
                BorderLayout.SOUTH);
        upcoming.setOpaque(false);
        upcoming.setLayout(new BoxLayout(upcoming, BoxLayout.Y_AXIS));
        Card upcomingCard = Card.titled("接下來 30 天", Widgets.scroll(upcoming));
        upcomingCard.setPreferredSize(new Dimension(330, 300));
        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.add(tableCard, BorderLayout.CENTER);
        body.add(upcomingCard, BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    private void withSelected(Consumer<RecurringRule> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            ui.showInfo("定期交易", "請先在表格中選擇一筆定期交易。");
            return;
        }
        action.accept(model.rows.get(table.convertRowIndexToModel(row)));
    }

    private void postDue() {
        ui.run(() -> {
            int posted = ui.app().recurring().postDue().size();
            ui.status(posted == 0 ? "目前沒有到期的定期交易。" : "已入帳 " + posted + " 筆定期交易（Ctrl+Z 可復原）");
        });
    }

    @Override
    public String id() {
        return "recurring";
    }

    @Override
    public String title() {
        return "定期";
    }

    @Override
    public String glyph() {
        return "定";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.RECURRING, Topic.WALLETS, Topic.CATEGORIES);
    }

    @Override
    protected void reload() {
        model.set(ui.app().recurring().all(), ui.app().categories().byId(), ui.app().transactions().walletsById());
        int due = ui.app().recurring().dueCount();
        postDue.setText(due == 0 ? "入帳到期項目" : "入帳到期項目（" + due + "）");
        postDue.setEnabled(due > 0);
        upcoming.removeAll();
        List<RecurringService.Occurrence> items = ui.app().recurring().upcoming(30);
        if (items.isEmpty()) {
            upcoming.add(Widgets.empty("接下來 30 天沒有定期交易"));
        }
        for (RecurringService.Occurrence o : items) {
            String when = o.date().format(DateTimeFormatter.ofPattern("M/d")) + "（"
                    + o.date().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.TAIWAN) + "）";
            upcoming.add(Rows.row(new BadgeIcon(o.rule().type() == TransactionType.TRANSFER ? "轉" : "定",
                            Palette.forType(o.rule().type()), 28), o.rule().name(), when,
                    (o.rule().type() == TransactionType.INCOME ? "+" : o.rule().type() == TransactionType.EXPENSE ? "-" : "")
                            + o.rule().amount().format(), () -> Palette.forType(o.rule().type())));
        }
    }

    private static final class Model extends AbstractTableModel {
        private static final String[] COLUMNS = {"名稱", "類型", "金額", "分類／轉入", "錢包", "週期", "下次日期", "狀態"};
        private List<RecurringRule> rows = List.of();
        private Map<Long, Category> categories = Map.of();
        private Map<Long, Wallet> wallets = Map.of();

        void set(List<RecurringRule> rows, Map<Long, Category> categories, Map<Long, Wallet> wallets) {
            this.rows = rows;
            this.categories = categories;
            this.wallets = wallets;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 3 ? Tables.Labeled.class : Object.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            RecurringRule r = rows.get(row);
            return switch (column) {
                case 0 -> r.name();
                case 1 -> r.type().displayName();
                case 2 -> r.amount();
                case 3 -> {
                    if (r.type() == TransactionType.TRANSFER) {
                        Wallet to = wallets.get(r.toWalletId());
                        yield new Tables.Labeled(new BadgeIcon("轉", Palette.transfer(), 22), "轉入 " + (to == null ? "?" : to.name()));
                    }
                    Category c = categories.get(r.categoryId());
                    yield c == null ? new Tables.Labeled(null, "?") : new Tables.Labeled(BadgeIcon.of(c, 22), c.name());
                }
                case 4 -> wallets.containsKey(r.walletId()) ? wallets.get(r.walletId()).name() : "?";
                case 5 -> r.scheduleText();
                case 6 -> r.nextDueDate().map(DATE::format).orElse("—");
                case 7 -> !r.active() ? "暫停" : r.isFinished() ? "已結束" : "啟用";
                default -> "";
            };
        }
    }
}
