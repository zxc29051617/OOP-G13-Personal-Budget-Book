package budgetbook.ui.forms;

import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Colors;
import budgetbook.service.AppContext;
import budgetbook.ui.FormDialog;
import budgetbook.ui.components.BadgeIcon;
import budgetbook.ui.components.ColorPicker;
import budgetbook.ui.components.FormPanel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.theme.Palette;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** 新增或編輯分類：名稱、支出或收入、圖示文字與顏色，旁邊即時預覽徽章。 */
public final class CategoryForm implements FormDialog.Form {
    private final AppContext app;
    private final Category existing;
    private final JTextField name = new JTextField(14);
    private final JComboBox<CategoryKind> kind = new JComboBox<>(CategoryKind.values());
    private final JTextField icon = new JTextField(3);
    private final ColorPicker color;
    private final JLabel preview = new JLabel();
    private final FormPanel form = new FormPanel();

    public CategoryForm(AppContext app, Category existing, CategoryKind presetKind) {
        this.app = app;
        this.existing = existing;
        color = new ColorPicker(existing == null ? Colors.paletteColor(app.categories().all().size()) : existing.color());
        form.add("名稱", name);
        form.add("類型", kind);
        form.add("圖示", Widgets.row(10, icon, preview));
        form.addHint("一到兩個字，可以是中文字、符號或 emoji；空白時使用名稱的第一個字。");
        form.add("顏色", color);
        kind.setSelectedItem(existing == null ? presetKind : existing.kind());
        if (existing != null) {
            name.setText(existing.name());
            icon.setText(existing.icon());
        }
        DocumentListener update = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }
        };
        name.getDocument().addDocumentListener(update);
        icon.getDocument().addDocumentListener(update);
        color.onChange(this::updatePreview);
        updatePreview();
    }

    private void updatePreview() {
        String glyph = icon.getText().isBlank()
                ? (name.getText().isBlank() ? "?" : name.getText().strip().substring(0, 1)) : icon.getText().strip();
        int end = glyph.offsetByCodePoints(0, Math.min(2, glyph.codePointCount(0, glyph.length())));
        preview.setIcon(new BadgeIcon(glyph.substring(0, end), Palette.of(color.color()), 34));
    }

    @Override
    public String title() {
        return existing == null ? "新增分類" : "編輯分類";
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
        CategoryKind selected = (CategoryKind) kind.getSelectedItem();
        if (existing == null) {
            app.categories().create(name.getText(), selected, color.color(), icon.getText());
        } else {
            app.categories().update(new Category(existing.id(), name.getText(), selected, color.color(), icon.getText()));
        }
    }
}
