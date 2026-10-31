package budgetbook.ui.forms;

import budgetbook.model.Colors;
import budgetbook.model.Money;
import budgetbook.model.SavingsGoal;
import budgetbook.service.AppContext;
import budgetbook.ui.FormDialog;
import budgetbook.ui.components.ColorPicker;
import budgetbook.ui.components.Fields;
import budgetbook.ui.components.FormPanel;

import javax.swing.JComponent;
import javax.swing.JTextField;

/** 新增或編輯儲蓄目標。 */
public final class GoalForm implements FormDialog.Form {
    private final AppContext app;
    private final SavingsGoal existing;
    private final JTextField name = new JTextField(16);
    private final Fields.AmountField target = new Fields.AmountField(null);
    private final Fields.DateField deadline = new Fields.DateField(null);
    private final ColorPicker color;
    private final FormPanel form = new FormPanel();

    public GoalForm(AppContext app, SavingsGoal existing) {
        this.app = app;
        this.existing = existing;
        color = new ColorPicker(existing == null ? Colors.paletteColor(app.goals().all().size() * 3) : existing.color());
        form.add("名稱", name);
        form.add("目標金額", target);
        form.add("目標日期", deadline);
        form.addHint("可以空白；有日期時會計算每個月要存多少。");
        form.add("顏色", color);
        if (existing != null) {
            name.setText(existing.name());
            target.setAmount(existing.target());
            deadline.setDate(existing.deadline());
        }
    }

    @Override
    public String title() {
        return existing == null ? "新增儲蓄目標" : "編輯儲蓄目標";
    }

    @Override
    public JComponent content() {
        return form;
    }

    @Override
    public JComponent initialFocus() {
        return name;
    }

    @Override
    public void save() {
        Money saved = existing == null ? Money.ZERO : existing.saved();
        app.goals().save(new SavingsGoal(existing == null ? 0 : existing.id(), name.getText(), target.amount(), saved,
                deadline.optionalDate("目標日期"), color.color()));
    }
}
