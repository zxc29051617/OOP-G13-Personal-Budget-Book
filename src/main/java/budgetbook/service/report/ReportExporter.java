package budgetbook.service.report;

import java.io.IOException;
import java.io.Writer;

/** Strategy：報表輸出格式。新增格式只要多寫一個實作。 */
public interface ReportExporter {
    /** 例如「CSV 試算表」。 */
    String displayName();

    /** 副檔名，不含點，例如 {@code csv}。 */
    String extension();

    void write(Report report, Writer out) throws IOException;
}
