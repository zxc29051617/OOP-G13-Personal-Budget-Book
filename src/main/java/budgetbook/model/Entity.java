package budgetbook.model;

/**
 * 有資料庫識別碼的實體。
 *
 * <p>所有實體都是不可變的 record；id 為 0 代表「尚未儲存」，儲存後由 repository 回傳帶有新 id 的複本。</p>
 */
public interface Entity {
    long id();

    default boolean isNew() {
        return id() == 0;
    }
}
