package budgetbook.ui;

import budgetbook.exception.BudgetBookException;
import budgetbook.model.Money;
import budgetbook.ui.components.Fields;
import budgetbook.ui.components.Widgets;

import java.util.Optional;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 簡單的輸入對話框，例如輸入預算金額或存入金額。 */
public final class Prompts {
    private Prompts() {
    }

    /**
     * 詢問金額。取消時回傳空值；{@code allowBlank} 時空白輸入代表 0（例如取消預算）。
     */
    public static Optional<Money> amount(UiContext ui, String title, String message, Money initial, boolean allowBlank) {
        Fields.AmountField field = new Fields.AmountField(initial);
        JPanel panel = Widgets.column(8, Widgets.label(message), field);
        while (true) {
            SwingUtilities.invokeLater(field::requestFocusInWindow);
            int choice = JOptionPane.showConfirmDialog(ui.window(), panel, title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            if (allowBlank && field.getText().isBlank()) {
                return Optional.of(Money.ZERO);
            }
            try {
                return Optional.of(field.amount());
            } catch (BudgetBookException e) {
                ui.showError(e.getMessage());
            }
        }
    }
}
