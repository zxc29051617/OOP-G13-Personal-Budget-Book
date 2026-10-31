package budgetbook.ui;

import budgetbook.event.Topic;
import budgetbook.ui.components.Widgets;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * 每一頁的共同父類別（Template Method）：子類別在建構時排版，
 * 在 {@link #reload()} 中重新讀取資料。資料變更事件來時，顯示中的頁面立即重新整理，
 * 其他頁面標記為過期，等切換過去時才重新整理。
 */
public abstract class Page extends JPanel {
    protected final UiContext ui;
    private boolean stale = true;

    protected Page(UiContext ui) {
        super(new BorderLayout(0, 14));
        this.ui = ui;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
    }

    /** 頁面代號，用於切換頁面。 */
    public abstract String id();

    /** 側邊欄與標題顯示的名稱。 */
    public abstract String title();

    /** 側邊欄圖示的字。 */
    public abstract String glyph();

    /** 哪些資料變更時需要重新整理。 */
    public abstract Set<Topic> topics();

    /** 重新讀取資料並更新畫面。 */
    protected abstract void reload();

    public final void refresh() {
        reload();
        stale = false;
        revalidate();
        repaint();
    }

    final void markStale() {
        stale = true;
    }

    final boolean isStale() {
        return stale;
    }

    /** 顯示頁面後讓第一個欄位取得焦點等等。 */
    public void onShown() {
    }

    /** 標題列：左邊是標題與說明，右邊是動作按鈕。 */
    protected JComponent header(String subtitle, JComponent... actions) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(Widgets.column(2, Widgets.title(title()), Widgets.muted(subtitle)), BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.setOpaque(false);
        for (JComponent action : actions) {
            buttons.add(action);
        }
        header.add(buttons, BorderLayout.EAST);
        return header;
    }
}
