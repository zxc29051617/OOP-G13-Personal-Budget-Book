package budgetbook.ui;

import budgetbook.exception.BudgetBookException;
import budgetbook.ui.components.ThemedLabel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * 表單對話框：Enter 儲存、Esc 取消；儲存失敗時把錯誤訊息顯示在對話框內，不另外跳視窗。
 */
public final class FormDialog extends JDialog {
    /** 對話框中的表單內容。 */
    public interface Form {
        String title();

        JComponent content();

        /** 開啟時取得焦點的欄位。 */
        JComponent initialFocus();

        /** 檢查並儲存；資料有誤時丟出例外，對話框會顯示訊息。 */
        void save();

        /** 有「儲存並新增下一筆」時回傳 true，並實作 {@link #reset()}。 */
        default boolean supportsSaveAndNew() {
            return false;
        }

        default void reset() {
        }
    }

    private final Form form;
    private final JLabel error = new ThemedLabel(" ", Fonts.small(), Palette::expense);
    private boolean saved;

    private FormDialog(Window owner, Form form) {
        super(owner, form.title(), ModalityType.APPLICATION_MODAL);
        this.form = form;
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));
        root.add(form.content(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.add(error, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (form.supportsSaveAndNew()) {
            JButton saveAndNew = Widgets.button("儲存並新增下一筆", this::saveAndNew);
            saveAndNew.setToolTipText("Ctrl+Enter");
            buttons.add(saveAndNew);
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                    Keys.menu()), "saveAndNew");
            root.getActionMap().put("saveAndNew", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveAndNew();
                }
            });
        }
        buttons.add(Widgets.button("取消", this::dispose));
        JButton save = Widgets.primary("儲存", this::save);
        buttons.add(save);
        bottom.add(buttons, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(save);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        root.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    /** 顯示對話框，儲存成功（至少一次）時回傳 true。 */
    public static boolean show(UiContext ui, Form form) {
        FormDialog dialog = new FormDialog(ui.window(), form);
        SwingUtilities.invokeLater(() -> form.initialFocus().requestFocusInWindow());
        dialog.setVisible(true);
        return dialog.saved;
    }

    private void save() {
        if (trySave()) {
            dispose();
        }
    }

    private void saveAndNew() {
        if (trySave()) {
            form.reset();
            error.setText("已儲存，可以繼續輸入下一筆。");
            form.initialFocus().requestFocusInWindow();
        }
    }

    private boolean trySave() {
        try {
            form.save();
            saved = true;
            error.setText(" ");
            return true;
        } catch (BudgetBookException e) {
            error.setText(e.getMessage());
            pack();
            return false;
        }
    }
}
