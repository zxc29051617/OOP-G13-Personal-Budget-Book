package budgetbook.ui.theme;

import budgetbook.model.BudgetLevel;
import budgetbook.model.TransactionType;
import com.formdev.flatlaf.FlatLaf;

import java.awt.Color;
import javax.swing.UIManager;

/** 依目前主題（淺色／深色）提供一致的顏色，畫面不直接寫死色碼。 */
public final class Palette {
    private Palette() {
    }

    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }

    private static Color pick(int light, int dark) {
        return new Color(isDark() ? dark : light);
    }

    public static Color accent() {
        return pick(0x3F8A57, 0x5FB27A);
    }

    public static Color accentSoft() {
        return pick(0xE2F0E5, 0x26392C);
    }

    public static Color income() {
        return pick(0x2E9E5B, 0x5CCB8A);
    }

    public static Color expense() {
        return pick(0xD64545, 0xFF7B7B);
    }

    public static Color transfer() {
        return pick(0x3B7DD8, 0x7AB0FF);
    }

    public static Color warning() {
        return pick(0xD98E04, 0xF2B84B);
    }

    public static Color forType(TransactionType type) {
        return switch (type) {
            case INCOME -> income();
            case EXPENSE -> expense();
            case TRANSFER -> transfer();
        };
    }

    public static Color forLevel(BudgetLevel level) {
        return switch (level) {
            case OK -> accent();
            case WARNING -> warning();
            case OVER -> expense();
        };
    }

    public static Color page() {
        return color("Panel.background", 0xF4F5F1);
    }

    public static Color card() {
        return pick(0xFFFFFF, 0x34373B);
    }

    public static Color sidebar() {
        return pick(0xE9EDE5, 0x26282B);
    }

    public static Color border() {
        return pick(0xDEE2D9, 0x45494E);
    }

    public static Color track() {
        return pick(0xE7EAE3, 0x45494E);
    }

    public static Color text() {
        return color("Label.foreground", 0x283028);
    }

    public static Color muted() {
        return pick(0x6F776C, 0xA2A8A0);
    }

    public static Color of(int rgb) {
        return new Color(rgb & 0xFFFFFF);
    }

    /** 在背景色上看得清楚的文字顏色（黑或白）。 */
    public static Color contrastText(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen()
                + 0.114 * background.getBlue()) / 255;
        return luminance > 0.62 ? new Color(0x283028) : Color.WHITE;
    }

    public static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color color(String key, int fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? new Color(fallback) : new Color(color.getRGB());
    }
}
