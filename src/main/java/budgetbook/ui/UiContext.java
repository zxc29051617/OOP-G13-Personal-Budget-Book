package budgetbook.ui;

import budgetbook.exception.BudgetBookException;
import budgetbook.service.AppContext;

import java.awt.Component;
import java.awt.Window;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 頁面共用的環境：取得 service、切換頁面、顯示訊息，並統一處理錯誤。
 * 頁面只呼叫 service，不直接碰資料庫。
 */
public final class UiContext {
    private final AppContext app;
    private final Supplier<Component> owner;
    private Consumer<String> navigator = id -> { };
    private Consumer<String> status = message -> { };

    public UiContext(AppContext app, Supplier<Component> owner) {
        this.app = app;
        this.owner = owner;
    }

    void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator;
    }

    void setStatus(Consumer<String> status) {
        this.status = status;
    }

    public AppContext app() {
        return app;
    }

    public Window window() {
        Component component = owner.get();
        return component == null ? null : SwingUtilities.getWindowAncestor(component);
    }

    public void navigate(String pageId) {
        navigator.accept(pageId);
    }

    public void status(String message) {
        status.accept(message);
    }

    /** 執行一個動作；業務規則錯誤以對話框顯示中文訊息。 */
    public boolean run(Runnable action) {
        try {
            action.run();
            return true;
        } catch (BudgetBookException e) {
            showError(e.getMessage());
            return false;
        } catch (RuntimeException e) {
            e.printStackTrace();
            showError("發生未預期的錯誤：" + e);
            return false;
        }
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(owner.get(), message, "無法完成", JOptionPane.WARNING_MESSAGE);
    }

    public void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(owner.get(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** 開啟表單對話框，儲存成功時回傳 true。 */
    public boolean open(FormDialog.Form form) {
        return FormDialog.show(this, form);
    }

    public boolean confirm(String title, String message) {
        return JOptionPane.showConfirmDialog(owner.get(), message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
