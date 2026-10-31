package budgetbook.ui.theme;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Set;
import javax.swing.UIManager;

/**
 * 字型：優先使用系統內建的繁體中文字型，避免中文顯示成方框。
 * 尺寸以 FlatLaf 的 defaultFont 為基準，所以會跟著系統縮放。
 */
public final class Fonts {
    private static final List<String> PREFERRED = List.of(
            "Microsoft JhengHei UI", "Microsoft JhengHei", "PingFang TC", "Noto Sans CJK TC", "Noto Sans TC",
            "Source Han Sans TC", "Heiti TC", "WenQuanYi Micro Hei", "Noto Sans CJK SC", "Droid Sans Fallback");

    private static String family;

    private Fonts() {
    }

    /** 第一個安裝在這台電腦上的中文字型；都沒有時使用 Java 的邏輯字型 SansSerif。 */
    public static synchronized String family() {
        if (family == null) {
            Set<String> installed = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames());
            family = PREFERRED.stream().filter(installed::contains).findFirst().orElse(Font.SANS_SERIF);
        }
        return family;
    }

    /** 以預設字級為 1.0 的相對大小，例如 {@code of(Font.BOLD, 1.6f)}。 */
    public static Font of(int style, float scale) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null) {
            base = new Font(family(), Font.PLAIN, 14);
        }
        return base.deriveFont(style, Math.round(base.getSize2D() * scale * 2) / 2f);
    }

    public static Font regular() {
        return of(Font.PLAIN, 1f);
    }

    public static Font bold() {
        return of(Font.BOLD, 1f);
    }

    public static Font small() {
        return of(Font.PLAIN, 0.86f);
    }
}
