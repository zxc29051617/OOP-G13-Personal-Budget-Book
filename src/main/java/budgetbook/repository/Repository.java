package budgetbook.repository;

import budgetbook.exception.EntityNotFoundException;
import budgetbook.model.Entity;

import java.util.List;
import java.util.Optional;

/**
 * Repository 模式：以集合的方式存取實體，呼叫端不需要知道背後是 SQLite 還是 MySQL。
 *
 * @param <T> 實體型別
 */
public interface Repository<T extends Entity> {
    Optional<T> findById(long id);

    List<T> findAll();

    /** id 為 0 時新增並回傳帶有新 id 的物件，否則更新。 */
    T save(T entity);

    /** 以原本的 id 重新寫回已刪除的資料，供復原（undo）使用。 */
    T restore(T entity);

    void deleteById(long id);

    long count();

    /** 中文名稱，用於錯誤訊息，例如「交易」。 */
    String entityName();

    default T getById(long id) {
        return findById(id).orElseThrow(() -> new EntityNotFoundException(entityName(), id));
    }
}
