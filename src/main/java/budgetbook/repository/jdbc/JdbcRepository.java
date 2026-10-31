package budgetbook.repository.jdbc;

import budgetbook.exception.EntityNotFoundException;
import budgetbook.model.Entity;
import budgetbook.repository.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Template Method：通用的 CRUD 流程寫在這裡，子類別只需提供資料表名稱、欄位、
 * 如何把一列資料轉成物件（{@link #map}）以及物件的欄位值（{@link #values}）。
 */
public abstract class JdbcRepository<T extends Entity> implements Repository<T> {
    protected final JdbcSession session;
    private final String table;
    private final String entityName;
    private final List<String> columns;
    private final String selectSql;

    protected JdbcRepository(JdbcSession session, String table, String entityName, String... columns) {
        this.session = session;
        this.table = table;
        this.entityName = entityName;
        this.columns = List.of(columns);
        this.selectSql = "SELECT " + columnList("") + " FROM " + table;
    }

    /** 把目前這一列轉成實體。 */
    protected abstract T map(ResultSet resultSet) throws SQLException;

    /** 依建構子給的欄位順序回傳要寫入的值（不含 id）。 */
    protected abstract Object[] values(T entity);

    protected abstract T withId(T entity, long id);

    /** findAll 的排序方式。 */
    protected String defaultOrder() {
        return "id";
    }

    /** {@code SELECT id, ... FROM table}，子類別可在後面接 WHERE。 */
    protected String select() {
        return selectSql;
    }

    /** 帶有表格別名前綴的欄位清單，例如 {@code t.id, t.name}。 */
    protected String columnList(String prefix) {
        return prefix + "id, " + columns.stream().map(c -> prefix + c).collect(Collectors.joining(", "));
    }

    @Override
    public Optional<T> findById(long id) {
        return session.queryOne(selectSql + " WHERE id = ?", this::map, id);
    }

    @Override
    public List<T> findAll() {
        return session.query(selectSql + " ORDER BY " + defaultOrder(), this::map);
    }

    @Override
    public T save(T entity) {
        return entity.isNew() ? insert(entity) : update(entity);
    }

    private T insert(T entity) {
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        long id = session.insert(sql, values(entity));
        return withId(entity, id);
    }

    private T update(T entity) {
        String assignments = columns.stream().map(c -> c + " = ?").collect(Collectors.joining(", "));
        Object[] values = values(entity);
        Object[] params = Arrays.copyOf(values, values.length + 1);
        params[values.length] = entity.id();
        if (session.update("UPDATE " + table + " SET " + assignments + " WHERE id = ?", params) == 0) {
            throw new EntityNotFoundException(entityName, entity.id());
        }
        return entity;
    }

    @Override
    public T restore(T entity) {
        String placeholders = String.join(", ", Collections.nCopies(columns.size() + 1, "?"));
        Object[] values = values(entity);
        Object[] params = new Object[values.length + 1];
        params[0] = entity.id();
        System.arraycopy(values, 0, params, 1, values.length);
        session.update("INSERT INTO " + table + " (id, " + String.join(", ", columns) + ") VALUES ("
                + placeholders + ")", params);
        return entity;
    }

    @Override
    public void deleteById(long id) {
        if (session.update("DELETE FROM " + table + " WHERE id = ?", id) == 0) {
            throw new EntityNotFoundException(entityName, id);
        }
    }

    @Override
    public long count() {
        return session.queryLong("SELECT COUNT(*) FROM " + table);
    }

    @Override
    public String entityName() {
        return entityName;
    }

    String table() {
        return table;
    }
}
