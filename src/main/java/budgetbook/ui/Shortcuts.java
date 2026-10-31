package budgetbook.ui;

import java.util.List;

/** 鍵盤快捷鍵清單（設定頁與 F1 說明共用）。macOS 上 Ctrl 換成 Command。 */
public final class Shortcuts {
    public record Shortcut(String keys, String description) {
    }

    public static final List<Shortcut> ALL = List.of(
            new Shortcut("Ctrl+N", "新增交易"),
            new Shortcut("Ctrl+T", "新增轉帳"),
            new Shortcut("Ctrl+Z", "復原"),
            new Shortcut("Ctrl+Y／Ctrl+Shift+Z", "重做"),
            new Shortcut("Ctrl+F", "搜尋交易"),
            new Shortcut("Ctrl+1 … Ctrl+0", "切換到第 1～10 頁"),
            new Shortcut("Ctrl+I／Ctrl+E", "匯入／匯出交易 CSV"),
            new Shortcut("Ctrl+Shift+D", "切換淺色／深色模式"),
            new Shortcut("Enter／Delete／Ctrl+D", "交易表格中：編輯／刪除／複製到今天"),
            new Shortcut("Ctrl+Enter", "交易視窗中：儲存並新增下一筆"),
            new Shortcut("↑ ↓", "日期欄：加減一天；時間欄：加減五分鐘"),
            new Shortcut("F1", "顯示快捷鍵"));

    private Shortcuts() {
    }

    public static String asText() {
        StringBuilder text = new StringBuilder();
        for (Shortcut shortcut : ALL) {
            text.append(String.format("%-22s %s%n", shortcut.keys(), shortcut.description()));
        }
        return text.toString();
    }
}
