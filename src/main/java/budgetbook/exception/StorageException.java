package budgetbook.exception;

/** 資料庫存取失敗，包裝底層的 {@link java.sql.SQLException}。 */
public class StorageException extends BudgetBookException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
