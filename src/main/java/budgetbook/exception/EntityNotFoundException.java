package budgetbook.exception;

/** 依 id 找不到資料，通常代表資料已被刪除。 */
public class EntityNotFoundException extends BudgetBookException {
    private final String entityName;
    private final long id;

    public EntityNotFoundException(String entityName, long id) {
        super("找不到" + entityName + "（編號 " + id + "），可能已被刪除。");
        this.entityName = entityName;
        this.id = id;
    }

    public String entityName() {
        return entityName;
    }

    public long id() {
        return id;
    }
}
