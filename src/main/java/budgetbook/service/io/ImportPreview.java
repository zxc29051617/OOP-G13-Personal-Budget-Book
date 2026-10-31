package budgetbook.service.io;

import java.util.List;

/** 匯入前的預覽：可以匯入的列、重複的列與錯誤訊息。 */
public record ImportPreview(List<ImportRow> rows, List<String> errors) {
    public ImportPreview {
        rows = List.copyOf(rows);
        errors = List.copyOf(errors);
    }

    public long newCount() {
        return rows.stream().filter(row -> !row.duplicate()).count();
    }

    public long duplicateCount() {
        return rows.stream().filter(ImportRow::duplicate).count();
    }
}
