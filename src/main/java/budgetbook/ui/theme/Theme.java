package budgetbook.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;

/** 套用 FlatLaf 淺色或深色主題，並設定綠色主色與中文字型。 */
public final class Theme {
    private Theme() {
    }

    public static void apply(boolean dark) {
        Map<String, String> extras = new HashMap<>();
        extras.put("@accentColor", dark ? "#5FB27A" : "#3F8A57");
        if (!dark) {
            extras.put("@background", "#F4F5F1");
        }
        // 比系統預設字級大 1，中文字比較清楚；以相對大小設定才會保留系統的縮放比例
        extras.put("defaultFont", "+1 \"" + Fonts.family() + "\"");
        // Windows 上 semibold 預設是 Segoe UI Semibold，沒有中文字，改成同字型的粗體
        extras.put("semibold.font", "bold");
        FlatLaf.setGlobalExtraDefaults(extras);
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("TitlePane.unifiedBackground", true);
    }

    /** 切換主題並更新所有已開啟的視窗。 */
    public static void switchTo(boolean dark) {
        apply(dark);
        FlatLaf.updateUI();
    }

    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }
}
