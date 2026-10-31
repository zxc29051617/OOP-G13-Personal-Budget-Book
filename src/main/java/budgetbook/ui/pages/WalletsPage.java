package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.ChartView;
import budgetbook.ui.components.Charts;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.ThemedLabel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.components.WrapLayout;
import budgetbook.ui.forms.TransactionForm;
import budgetbook.ui.forms.WalletForm;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/** 錢包：每個錢包的餘額卡片、轉帳，以及近 90 天總資產趨勢。 */
public final class WalletsPage extends Page {
    private final JPanel cards = new JPanel(new WrapLayout(FlowLayout.LEFT, 14, 14));
    private final JLabel total = Widgets.muted("");
    private final ChartView trend = new ChartView(230);

    public WalletsPage(UiContext ui) {
        super(ui);
        add(header("現金、銀行帳戶、電子支付與信用卡集中管理",
                Widgets.button("轉帳", () -> ui.open(new TransactionForm(ui.app(), null, TransactionType.TRANSFER))),
                Widgets.primary("＋ 新增錢包", () -> ui.open(new WalletForm(ui.app(), null)))), BorderLayout.NORTH);
        cards.setOpaque(false);
        ScrollablePanel body = new ScrollablePanel(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        total.setAlignmentX(Component.LEFT_ALIGNMENT);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(total);
        body.add(cards);
        body.add(Box.createVerticalStrut(8));
        Card trendCard = Card.titled("近 90 天總資產", trend);
        trendCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(trendCard);
        add(Widgets.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public String id() {
        return "wallets";
    }

    @Override
    public String title() {
        return "錢包";
    }

    @Override
    public String glyph() {
        return "包";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.of(Topic.WALLETS, Topic.TRANSACTIONS);
    }

    @Override
    protected void reload() {
        Map<Long, Money> balances = ui.app().wallets().balances();
        cards.removeAll();
        for (Wallet wallet : ui.app().wallets().all()) {
            cards.add(card(wallet, balances.getOrDefault(wallet.id(), Money.ZERO)));
        }
        cards.add(addCard());
        total.setText("總資產 " + ui.app().wallets().totalBalance().format() + "，共 "
                + ui.app().wallets().active().size() + " 個使用中的錢包");
        LocalDate today = LocalDate.now(ui.app().clock());
        trend.setChart(Charts.balanceLine(ui.app().statistics().balanceTrend(DateRange.lastDays(today, 90))));
    }

    private JComponent card(Wallet wallet, Money balance) {
        Card card = new Card(new BorderLayout(10, 8));
        card.setPreferredSize(new Dimension(250, 132));
        JLabel name = new ThemedLabel(wallet.name(), Fonts.of(Font.BOLD, 1.05f),
                wallet.archived() ? Palette::muted : Palette::text);
        name.setIcon(BadgeIcon.of(wallet, 34));
        name.setIconTextGap(10);
        JButton more = Widgets.link("⋯", () -> { });
        more.addActionListener(e -> menu(wallet).show(more, 0, more.getHeight()));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        top.add(more, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        card.add(Widgets.value(balance.format(), 1.45f, balance.isNegative() ? Palette::expense : Palette::text),
                BorderLayout.CENTER);
        card.add(Widgets.muted(wallet.type().displayName() + " · 初始 " + wallet.initialBalance().format()
                + (wallet.archived() ? " · 已封存" : "")), BorderLayout.SOUTH);
        return card;
    }

    private JPopupMenu menu(Wallet wallet) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(item("查看交易", () -> {
            ui.navigate("transactions");
            ui.app().bus().publish(new FilterRequest(null, wallet));
        }));
        menu.add(item("編輯", () -> ui.open(new WalletForm(ui.app(), wallet))));
        menu.add(item(wallet.archived() ? "取消封存" : "封存", () -> ui.run(
                () -> ui.app().wallets().setArchived(wallet.id(), !wallet.archived()))));
        menu.addSeparator();
        menu.add(item("刪除", () -> ui.run(() -> ui.app().wallets().delete(wallet.id()))));
        return menu;
    }

    private JComponent addCard() {
        JButton add = Widgets.button("＋ 新增錢包", () -> ui.open(new WalletForm(ui.app(), null)));
        add.setPreferredSize(new Dimension(250, 132));
        return add;
    }

    static JMenuItem item(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }
}
