package budgetbook.ui.pages;

import budgetbook.model.Category;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.model.Wallet;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.ThemedLabel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 清單中的一列：圖示、標題、說明與右側金額。 */
final class Rows {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("M/d HH:mm");

    private Rows() {
    }

    static JComponent row(Icon icon, String title, String subtitle, String amount, Supplier<Color> amountColor) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        if (icon != null) {
            row.add(new JLabel(icon), BorderLayout.WEST);
        }
        JLabel titleLabel = new ThemedLabel(title, Fonts.bold(), Palette::text);
        row.add(Widgets.column(1, titleLabel, Widgets.muted(subtitle)), BorderLayout.CENTER);
        if (amount != null) {
            JLabel value = new ThemedLabel(amount, Fonts.of(Font.BOLD, 1.0f), amountColor);
            row.add(value, BorderLayout.EAST);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    /** 一筆交易：分類圖示、分類與備註、時間與錢包、帶正負號的金額。 */
    static JComponent transaction(Transaction t, Map<Long, Category> categories, Map<Long, Wallet> wallets) {
        Icon icon;
        String title;
        String walletText;
        if (t.isTransfer()) {
            icon = new BadgeIcon("轉", Palette.transfer(), 32);
            title = "轉帳";
            walletText = name(wallets, t.walletId()) + " → " + name(wallets, t.toWalletId());
        } else {
            Category category = categories.get(t.categoryId());
            icon = category == null ? new BadgeIcon("?", Palette.muted(), 32) : BadgeIcon.of(category, 32);
            title = category == null ? "未分類" : category.name();
            walletText = name(wallets, t.walletId());
        }
        if (!t.note().isBlank()) {
            title += " · " + t.note();
        }
        String subtitle = t.dateTime().format(WHEN) + " · " + walletText + (t.recurringRuleId() != null ? " · 定期" : "");
        return row(icon, title, subtitle, amountText(t), () -> Palette.forType(t.type()));
    }

    static String amountText(Transaction t) {
        Money amount = t.amount();
        return switch (t.type()) {
            case INCOME -> "+" + amount.format();
            case EXPENSE -> "-" + amount.format();
            case TRANSFER -> amount.format();
        };
    }

    private static String name(Map<Long, Wallet> wallets, Long id) {
        Wallet wallet = id == null ? null : wallets.get(id);
        return wallet == null ? "?" : wallet.name();
    }
}
