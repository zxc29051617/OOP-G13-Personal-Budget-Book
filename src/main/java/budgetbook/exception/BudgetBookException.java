package budgetbook.exception;

/**
 * 本程式所有自訂例外的共同父類別。
 *
 * <p>使用 unchecked exception，讓 service 可以直接往上拋，由 UI 統一顯示錯誤訊息；
 * 訊息一律是可以直接給使用者看的繁體中文。</p>
 */
public class BudgetBookException extends RuntimeException {
    public BudgetBookException(String message) {
        super(message);
    }

    public BudgetBookException(String message, Throwable cause) {
        super(message, cause);
    }
}
