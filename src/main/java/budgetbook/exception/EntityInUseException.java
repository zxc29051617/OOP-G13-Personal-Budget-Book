package budgetbook.exception;

/** 資料仍被其他資料引用而不能刪除，例如錢包還有交易紀錄。 */
public class EntityInUseException extends BudgetBookException {
    public EntityInUseException(String message) {
        super(message);
    }
}
