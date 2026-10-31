package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.Category;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.PeriodSummary;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.service.StatisticsService;
import budgetbook.ui.DataActions;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.Fields;
import budgetbook.ui.components.Tables;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.forms.TransactionForm;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/** 交易紀錄：搜尋、依期間／類型／分類／錢包篩選、點欄位標題排序，以及新增、編輯、刪除、複製。 */
public final class TransactionsPage extends Page {
    private static final String ALL = "全部";
    private static final List<String> PERIODS = List.of("本月", "上個月", "最近 30 天", "今年", "全部期間", "自訂期間");

    private final JTextField search = new JTextField(16);
    private final JComboBox<String> period = new JComboBox<>(PERIODS.toArray(String[]::new));
    private final Fields.DateField from;
    private final Fields.DateField to;
    private final JComboBox<Object> type = new JComboBox<>(new Object[]{ALL, TransactionType.EXPENSE,
            TransactionType.INCOME, TransactionType.TRANSFER});
    private final JComboBox<Object> category = new JComboBox<>();
    private final JComboBox<Object> wallet = new JComboBox<>();
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final JLabel summary = Widgets.muted(" ");
    private final Timer searchDelay = new Timer(250, e -> refresh());
    private boolean updatingFilters;
    private List<Transaction> shown = List.of();

    public TransactionsPage(UiContext ui) {
        super(ui);
        LocalDate today = LocalDate.now(ui.app().clock());
        from = new Fields.DateField(today.withDayOfMonth(1));
        to = new Fields.DateField(today);
        add(header("搜尋、篩選與管理所有收入、支出和轉帳",
                Widgets.button("匯入 CSV", () -> DataActions.importTransactions(ui)),
                Widgets.button("匯出 CSV", () -> DataActions.exportTransactions(ui, shown)),
                Widgets.primary("＋ 新增交易", () -> ui.open(new TransactionForm(ui.app(), null, TransactionType.EXPENSE)))),
                BorderLayout.NORTH);

        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "搜尋備註、分類、錢包");
        search.setToolTipText("搜尋（Ctrl+F）");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        searchDelay.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchDelay.restart();
            }
        });
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.setIcon(null);
                if (value instanceof Category c) {
                    label.setText(c.name() + "（" + c.kind().displayName() + "）");
                    label.setIcon(BadgeIcon.of(c, 18));
                } else if (value instanceof Wallet w) {
                    label.setText(w.name());
                    label.setIcon(BadgeIcon.of(w, 18));
                }
                return label;
            }
        };
        category.setRenderer(renderer);
        wallet.setRenderer(renderer);
        for (JComboBox<?> combo : List.of(period, type, category, wallet)) {
            combo.addActionListener(e -> {
                if (!updatingFilters) {
                    refresh();
                }
            });
        }
        from.addActionListener(e -> refresh());
        to.addActionListener(e -> refresh());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(search);
        filters.add(period);
        filters.add(from);
        filters.add(to);
        filters.add(Widgets.muted("類型"));
        filters.add(type);
        filters.add(Widgets.muted("分類"));
        filters.add(category);
        filters.add(Widgets.muted("錢包"));
        filters.add(wallet);

        configureTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(summary, BorderLayout.WEST);
        footer.add(Widgets.muted("Enter 編輯 · Delete 刪除 · Ctrl+D 複製到今天 · Ctrl+Z 復原"), BorderLayout.EAST);

        ui.app().bus().subscribe(FilterRequest.class, request -> filterBy(request.category(), request.wallet()));

        Card card = new Card(new BorderLayout(0, 10));
        card.add(filters, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    private void configureTable() {
        Tables.style(table);
        TableRowSorter<Model> sorter = new TableRowSorter<>(model);
        sorter.setComparator(0, Comparator.naturalOrder());
        table.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING),
                new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        table.getColumnModel().getColumn(2).setCellRenderer(Tables.textRenderer(row -> Palette.forType(model.row(row).type())));
        table.getColumnModel().getColumn(6).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focused,
                                                           int row, int column) {
                Transaction tx = model.row(t.convertRowIndexToModel(row));
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, Rows.amountText(tx), selected, focused,
                        row, column);
                label.setHorizontalAlignment(JLabel.RIGHT);
                label.setFont(Fonts.bold());
                label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 14));
                if (!selected) {
                    label.setForeground(Palette.forType(tx.type()));
                }
                return label;
            }
        });
        int[] widths = {100, 64, 64, 150, 170, 260, 120};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelected();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                popup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                popup(e);
            }

            private void popup(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && !table.isRowSelected(row)) {
                    table.setRowSelectionInterval(row, row);
                }
                JPopupMenu menu = new JPopupMenu();
                menu.add(item("編輯", TransactionsPage.this::editSelected));
                menu.add(item("複製到今天", TransactionsPage.this::duplicateSelected));
                menu.addSeparator();
                menu.add(item("刪除", TransactionsPage.this::deleteSelected));
                menu.show(table, e.getX(), e.getY());
            }
        });
        bind("edit", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), this::editSelected);
        bind("delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), this::deleteSelected);
        bind("duplicate", KeyStroke.getKeyStroke(KeyEvent.VK_D, budgetbook.ui.Keys.menu()), this::duplicateSelected);
    }

    private static JMenuItem item(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    private void bind(String name, KeyStroke key, Runnable action) {
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, name);
        table.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private List<Transaction> selected() {
        List<Transaction> result = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            result.add(model.row(table.convertRowIndexToModel(viewRow)));
        }
        return result;
    }

    private void editSelected() {
        List<Transaction> rows = selected();
        if (rows.size() == 1) {
            ui.open(new TransactionForm(ui.app(), rows.get(0), rows.get(0).type()));
        }
    }

    private void deleteSelected() {
        List<Transaction> rows = selected();
        if (!rows.isEmpty()) {
            ui.run(() -> ui.app().transactions().deleteAll(rows.stream().map(Transaction::id).toList()));
        }
    }

    private void duplicateSelected() {
        List<Transaction> rows = selected();
        if (rows.size() == 1) {
            ui.run(() -> ui.app().transactions().duplicateToToday(rows.get(0).id()));
        }
    }

    /** 讓搜尋框取得焦點（Ctrl+F）。 */
    public void focusSearch() {
        search.requestFocusInWindow();
        search.selectAll();
    }

    /** 從其他頁面跳過來時，只看某個分類或錢包。 */
    public void filterBy(Category categoryFilter, Wallet walletFilter) {
        updatingFilters = true;
        period.setSelectedItem("全部期間");
        type.setSelectedIndex(0);
        search.setText("");
        refreshFilterChoices();
        selectById(category, categoryFilter == null ? null : categoryFilter.id());
        selectById(wallet, walletFilter == null ? null : walletFilter.id());
        updatingFilters = false;
        refresh();
    }

    @Override
    public String id() {
        return "transactions";
    }

    @Override
    public String title() {
        return "交易";
    }

    @Override
    public String glyph() {
        return "帳";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.TRANSACTIONS, Topic.WALLETS, Topic.CATEGORIES);
    }

    @Override
    protected void reload() {
        updatingFilters = true;
        refreshFilterChoices();
        boolean custom = "自訂期間".equals(period.getSelectedItem());
        from.setVisible(custom);
        to.setVisible(custom);
        updatingFilters = false;

        TransactionQuery query = TransactionQuery.all().withText(search.getText());
        DateRange range = selectedRange();
        if (range != null) {
            query = query.withRange(range);
        }
        if (type.getSelectedItem() instanceof TransactionType t) {
            query = query.withType(t);
        }
        if (category.getSelectedItem() instanceof Category c) {
            query = query.withCategory(c.id());
        }
        if (wallet.getSelectedItem() instanceof Wallet w) {
            query = query.withWallet(w.id());
        }
        shown = ui.app().transactions().search(query);
        model.setRows(shown, ui.app().categories().byId(), ui.app().transactions().walletsById());
        PeriodSummary totals = StatisticsService.summarize(range == null ? DateRange.ofDay(LocalDate.now(ui.app().clock()))
                : range, shown);
        Money net = totals.net();
        summary.setText("共 " + shown.size() + " 筆　收入 " + totals.income().format() + "　支出 "
                + totals.expense().format() + "　淨額 " + net.format());
    }

    private DateRange selectedRange() {
        LocalDate today = LocalDate.now(ui.app().clock());
        YearMonth month = YearMonth.from(today);
        return switch (String.valueOf(period.getSelectedItem())) {
            case "本月" -> DateRange.ofMonth(month);
            case "上個月" -> DateRange.ofMonth(month.minusMonths(1));
            case "最近 30 天" -> DateRange.lastDays(today, 30);
            case "今年" -> DateRange.ofYear(today.getYear());
            case "自訂期間" -> {
                try {
                    LocalDate start = from.date("開始日期");
                    LocalDate end = to.date("結束日期");
                    yield end.isBefore(start) ? DateRange.of(end, start) : DateRange.of(start, end);
                } catch (RuntimeException e) {
                    yield DateRange.ofMonth(month);
                }
            }
            default -> null;
        };
    }

    private void refreshFilterChoices() {
        Object selectedCategory = category.getSelectedItem();
        Object selectedWallet = wallet.getSelectedItem();
        category.removeAllItems();
        category.addItem(ALL);
        ui.app().categories().all().forEach(category::addItem);
        wallet.removeAllItems();
        wallet.addItem(ALL);
        ui.app().wallets().all().forEach(wallet::addItem);
        selectById(category, selectedCategory instanceof Category c ? c.id() : null);
        selectById(wallet, selectedWallet instanceof Wallet w ? w.id() : null);
    }

    private static void selectById(JComboBox<Object> combo, Long id) {
        combo.setSelectedIndex(0);
        if (id == null) {
            return;
        }
        for (int i = 1; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            long itemId = item instanceof Category c ? c.id() : item instanceof Wallet w ? w.id() : -1;
            if (itemId == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** 表格資料模型。 */
    private static final class Model extends AbstractTableModel {
        private static final String[] COLUMNS = {"日期", "時間", "類型", "分類", "錢包", "備註", "金額"};
        private List<Transaction> rows = List.of();
        private Map<Long, Category> categories = Map.of();
        private Map<Long, Wallet> wallets = Map.of();

        void setRows(List<Transaction> rows, Map<Long, Category> categories, Map<Long, Wallet> wallets) {
            this.rows = rows;
            this.categories = categories;
            this.wallets = wallets;
            fireTableDataChanged();
        }

        Transaction row(int index) {
            return rows.get(index);
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
            return switch (column) {
                case 0 -> LocalDate.class;
                case 1 -> LocalTime.class;
                case 3 -> Tables.Labeled.class;
                case 6 -> Money.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int column) {
            Transaction t = rows.get(rowIndex);
            return switch (column) {
                case 0 -> t.date();
                case 1 -> t.time();
                case 2 -> t.type().displayName();
                case 3 -> {
                    if (t.isTransfer()) {
                        yield new Tables.Labeled(new BadgeIcon("轉", Palette.transfer(), 22), "轉帳");
                    }
                    Category c = categories.get(t.categoryId());
                    yield c == null ? new Tables.Labeled(null, "?") : new Tables.Labeled(BadgeIcon.of(c, 22), c.name());
                }
                case 4 -> t.isTransfer() ? name(t.walletId()) + " → " + name(t.toWalletId()) : name(t.walletId());
                case 5 -> t.note() + (t.recurringRuleId() != null ? "（定期）" : "");
                case 6 -> t.isExpense() ? t.amount().negate() : t.amount();
                default -> "";
            };
        }

        private String name(Long id) {
            Wallet wallet = id == null ? null : wallets.get(id);
            return wallet == null ? "?" : wallet.name();
        }
    }
}
