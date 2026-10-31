package budgetbook.exception;

/** 輸入資料不合法，例如金額不是正數、名稱空白。{@link #field()} 指出是哪個欄位。 */
public class ValidationException extends BudgetBookException {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
