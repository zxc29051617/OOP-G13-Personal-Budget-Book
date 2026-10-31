package budgetbook.ui;

import budgetbook.event.CommandEvent;
import budgetbook.event.DataChangedEvent;
import budgetbook.event.EventBus;
import budgetbook.event.Topic;
import budgetbook.repository.StorageInfo;
import budgetbook.service.AppContext;
import budgetbook.ui.pages.AssistantPage;
import budgetbook.ui.pages.BudgetsPage;
import budgetbook.ui.pages.CategoriesPage;
import budgetbook.ui.pages.DashboardPage;
import budgetbook.ui.pages.ForestPage;
import budgetbook.ui.pages.GoalsPage;
import budgetbook.ui.pages.RecurringPage;
import budgetbook.ui.pages.ReportsPage;
import budgetbook.ui.pages.SettingsPage;
import budgetbook.ui.pages.TransactionsPage;
import budgetbook.ui.pages.WalletsPage;
import budgetbook.ui.theme.Theme;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * 主畫面：左邊導覽列、中間目前的頁面、下方狀態列。
 * 訂閱 {@link DataChangedEvent}（Observer）：顯示中的頁面立刻更新，其他頁面切換過去時才更新。
 * 不包含 JFrame，所以可以在沒有螢幕的環境中畫成圖片（README 截圖就是這樣產生的）。
 */
public final class AppShell extends JPanel {
    private final AppContext app;
    private final UiContext ui;
    private final List<Page> pages;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Sidebar sidebar;
    private final StatusBar statusBar;
    private final List<EventBus.Subscription> subscriptions = new ArrayList<>();
    private Page current;

    public AppShell(AppContext app) {
        super(new BorderLayout());
        this.app = app;
        this.ui = new UiContext(app, () -> this);
        pages = List.of(new DashboardPage(ui), new TransactionsPage(ui), new WalletsPage(ui), new CategoriesPage(ui),
                new BudgetsPage(ui), new RecurringPage(ui), new GoalsPage(ui), new ReportsPage(ui), new ForestPage(ui),
                new AssistantPage(ui), new SettingsPage(ui, this::setDarkTheme));
        for (Page page : pages) {
            content.add(page, page.id());
        }
        sidebar = new Sidebar(pages, this::show);
        StorageInfo info = app.storage().info();
        statusBar = new StatusBar(info.backend() + " · " + info.location());
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        ui.setNavigator(this::show);
        ui.setStatus(statusBar::show);
        subscriptions.add(app.bus().subscribe(DataChangedEvent.class, this::onDataChanged));
        subscriptions.add(app.bus().subscribe(CommandEvent.class, event -> statusBar.show(event.message()
                + (event.kind() == CommandEvent.Kind.EXECUTED ? "　（Ctrl+Z 復原）" : ""))));
        sidebar.updateTree(app.trees().currentTree());
        show(pages.get(0).id());
    }

    public UiContext ui() {
        return ui;
    }

    public List<Page> pages() {
        return pages;
    }

    public Page current() {
        return current;
    }

    @SuppressWarnings("unchecked")
    public <T extends Page> T page(Class<T> type) {
        return (T) pages.stream().filter(type::isInstance).findFirst().orElseThrow();
    }

    /** 切換到指定頁面；過期的頁面在顯示前重新整理。 */
    public void show(String id) {
        Page page = pages.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown page: " + id));
        current = page;
        if (page.isStale()) {
            page.refresh();
        }
        cards.show(content, id);
        sidebar.select(id);
        page.onShown();
    }

    private void onDataChanged(DataChangedEvent event) {
        for (Page page : pages) {
            if (event.affectsAny(page.topics())) {
                if (page == current) {
                    page.refresh();
                } else {
                    page.markStale();
                }
            }
        }
        if (event.affects(Topic.TRANSACTIONS) || event.affects(Topic.SETTINGS)) {
            sidebar.updateTree(app.trees().currentTree());
        }
    }

    /** 切換淺色／深色主題，並重新產生所有頁面上的圖表顏色。 */
    public void setDarkTheme(boolean dark) {
        if (dark != Theme.isDark()) {
            Theme.switchTo(dark);
            // 視窗中的元件已由 FlatLaf 更新；沒有視窗時（例如產生截圖）在這裡更新
            javax.swing.SwingUtilities.updateComponentTreeUI(this);
        }
        app.settings().setDarkTheme(dark);
        sidebar.updateIcons(pages);
        pages.forEach(Page::markStale);
        current.refresh();
        page(SettingsPage.class).syncTheme();
    }

    public void showStatus(String message) {
        statusBar.show(message);
    }

    /** 關閉前取消事件訂閱。 */
    public void dispose() {
        subscriptions.forEach(EventBus.Subscription::close);
    }
}
