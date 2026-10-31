package budgetbook.ui;

import budgetbook.event.HistoryChangedEvent;
import budgetbook.model.TransactionType;
import budgetbook.ui.forms.TransactionForm;
import budgetbook.ui.pages.TransactionsPage;
import budgetbook.ui.theme.Theme;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

/**
 * 選單與快捷鍵。每個動作都是一個 Swing {@link Action}（也是 Command 模式的一種），
 * 選單項目、按鈕與快捷鍵共用同一個物件，啟用狀態會一起改變。
 */
final class AppActions {
    private final AppShell shell;
    private final UiContext ui;
    private final Runnable quit;
    private final Action undo;
    private final Action redo;

    AppActions(AppShell shell, Runnable quit) {
        this.shell = shell;
        this.ui = shell.ui();
        this.quit = quit;
        undo = action("復原", key(KeyEvent.VK_Z, Keys.menu()), () -> ui.run(() -> ui.app().commands().undo()));
        redo = action("重做", key(KeyEvent.VK_Y, Keys.menu()), () -> ui.run(() -> ui.app().commands().redo()));
        undo.setEnabled(false);
        redo.setEnabled(false);
        ui.app().bus().subscribe(HistoryChangedEvent.class, this::onHistoryChanged);
    }

    private void onHistoryChanged(HistoryChangedEvent event) {
        undo.setEnabled(event.canUndo());
        undo.putValue(Action.NAME, event.canUndo() ? "復原：" + shorten(event.undoLabel()) : "復原");
        redo.setEnabled(event.canRedo());
        redo.putValue(Action.NAME, event.canRedo() ? "重做：" + shorten(event.redoLabel()) : "重做");
    }

    private static String shorten(String text) {
        return text.length() > 24 ? text.substring(0, 24) + "…" : text;
    }

    JMenuBar menuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("檔案");
        file.add(action("新增交易", key(KeyEvent.VK_N, Keys.menu()),
                () -> ui.open(new TransactionForm(ui.app(), null, TransactionType.EXPENSE))));
        file.add(action("新增轉帳", key(KeyEvent.VK_T, Keys.menu()),
                () -> ui.open(new TransactionForm(ui.app(), null, TransactionType.TRANSFER))));
        file.addSeparator();
        file.add(action("匯入交易 CSV…", key(KeyEvent.VK_I, Keys.menu()), () -> DataActions.importTransactions(ui)));
        file.add(action("匯出全部交易 CSV…", key(KeyEvent.VK_E, Keys.menu()), () -> DataActions.exportTransactions(ui,
                ui.app().transactions().search(budgetbook.model.TransactionQuery.all()))));
        file.add(action("月報與年報…", null, () -> shell.show("reports")));
        file.addSeparator();
        file.add(action("載入示範資料…", null, () -> DataActions.loadDemoData(ui)));
        file.addSeparator();
        file.add(action("結束", key(KeyEvent.VK_Q, Keys.menu()), quit));
        bar.add(file);

        JMenu edit = new JMenu("編輯");
        edit.add(undo);
        edit.add(redo);
        edit.addSeparator();
        edit.add(action("搜尋交易", key(KeyEvent.VK_F, Keys.menu()), () -> {
            shell.show("transactions");
            shell.page(TransactionsPage.class).focusSearch();
        }));
        bar.add(edit);

        JMenu view = new JMenu("檢視");
        List<Page> pages = shell.pages();
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            KeyStroke stroke = i < 10 ? key(KeyEvent.VK_0 + (i + 1) % 10, Keys.menu()) : null;
            view.add(action(page.title(), stroke, () -> shell.show(page.id())));
        }
        view.addSeparator();
        view.add(action("切換淺色／深色模式", key(KeyEvent.VK_D, Keys.shiftMenu()),
                () -> shell.setDarkTheme(!Theme.isDark())));
        bar.add(view);

        JMenu help = new JMenu("說明");
        help.add(action("鍵盤快捷鍵", key(KeyEvent.VK_F1, 0), this::showShortcuts));
        help.add(action("關於個人記帳本", null, () -> shell.show("settings")));
        bar.add(help);
        return bar;
    }

    /** Ctrl+Shift+Z 也可以重做（macOS 與許多軟體的習慣）。 */
    void bindExtraKeys(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key(KeyEvent.VK_Z, Keys.shiftMenu()), "redo");
        root.getActionMap().put("redo", redo);
    }

    private void showShortcuts() {
        JTextArea text = new JTextArea(Shortcuts.asText());
        text.setEditable(false);
        text.setFont(budgetbook.ui.theme.Fonts.regular());
        JOptionPane.showMessageDialog(ui.window(), new JScrollPane(text), "鍵盤快捷鍵", JOptionPane.PLAIN_MESSAGE);
    }

    private static KeyStroke key(int code, int modifiers) {
        return KeyStroke.getKeyStroke(code, modifiers);
    }

    private static Action action(String name, KeyStroke accelerator, Runnable runnable) {
        AbstractAction action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        };
        if (accelerator != null) {
            action.putValue(Action.ACCELERATOR_KEY, accelerator);
        }
        return action;
    }
}
