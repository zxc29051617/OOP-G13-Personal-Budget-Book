package budgetbook.ui;

import budgetbook.service.AppContext;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/** 主視窗：放入 {@link AppShell} 並加上選單列。 */
public final class MainFrame extends JFrame {
    private final AppContext app;
    private final AppShell shell;

    public MainFrame(AppContext app, boolean demo) {
        super(demo ? "個人記帳本（示範模式，資料不會存檔）" : "個人記帳本");
        this.app = app;
        shell = new AppShell(app);
        AppActions actions = new AppActions(shell, this::quit);
        setJMenuBar(actions.menuBar());
        actions.bindExtraKeys(shell);
        setContentPane(shell);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        setMinimumSize(new Dimension(1040, 680));
        setSize(new Dimension(1320, 840));
        setLocationRelativeTo(null);
    }

    public AppShell shell() {
        return shell;
    }

    private void quit() {
        shell.dispose();
        dispose();
        app.close();
        System.exit(0);
    }
}
