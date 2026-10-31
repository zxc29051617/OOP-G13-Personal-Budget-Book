package budgetbook.exception;

/** CSV 檔案格式錯誤，{@link #line()} 是出錯的行號（從 1 開始）。 */
public class CsvFormatException extends BudgetBookException {
    private final int line;

    public CsvFormatException(int line, String message) {
        super("第 " + line + " 行：" + message);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
