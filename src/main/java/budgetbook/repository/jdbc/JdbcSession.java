package budgetbook.repository.jdbc;

import budgetbook.exception.StorageException;
import budgetbook.model.Money;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 管理一條資料庫連線，並提供查詢小工具（類似精簡版 JdbcTemplate），
 * 讓各個 repository 不必重複撰寫 try-with-resources 與參數綁定。
 *
 * <p>桌面程式只有一位使用者，所有操作都在 Swing 的事件執行緒，所以一條連線就足夠。</p>
 */
public final class JdbcSession implements AutoCloseable {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final long VALIDATE_AFTER_IDLE_MILLIS = 60_000;

    private final SqlDialect dialect;
    private Connection connection;
    private long lastUsed;
    private int transactionDepth;

    public JdbcSession(SqlDialect dialect) {
        this.dialect = dialect;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    Connection connection() {
        try {
            long now = System.currentTimeMillis();
            boolean stale = connection != null && transactionDepth == 0 && dialect.canReconnect()
                    && now - lastUsed > VALIDATE_AFTER_IDLE_MILLIS && !connection.isValid(3);
            if (connection == null || connection.isClosed() || stale) {
                if (connection != null && !dialect.canReconnect()) {
                    throw new StorageException("記憶體資料庫的連線已關閉。", null);
                }
                closeQuietly();
                connection = dialect.openConnection();
                dialect.configure(connection);
            }
            lastUsed = now;
            return connection;
        } catch (SQLException e) {
            throw new StorageException("無法連線到 " + dialect.name() + " 資料庫：" + e.getMessage(), e);
        }
    }

    public <T> T inTransaction(Supplier<T> work) {
        Connection c = connection();
        boolean outermost = transactionDepth == 0;
        try {
            if (outermost) {
                c.setAutoCommit(false);
            }
            transactionDepth++;
            T result;
            try {
                result = work.get();
            } finally {
                transactionDepth--;
            }
            if (outermost) {
                c.commit();
            }
            return result;
        } catch (RuntimeException e) {
            if (outermost) {
                rollbackQuietly(c);
            }
            throw e;
        } catch (SQLException e) {
            if (outermost) {
                rollbackQuietly(c);
            }
            throw new StorageException("資料庫交易失敗：" + e.getMessage(), e);
        } finally {
            if (outermost) {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // 連線可能已中斷，下一次使用時會重新連線
                }
            }
        }
    }

    public void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw failure("查詢", e);
        }
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long queryLong(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    public int update(String sql, Object... params) {
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("寫入", e);
        }
    }

    /** 執行 INSERT 並回傳資料庫產生的 id。 */
    public long insert(String sql, Object... params) {
        try (PreparedStatement statement = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("資料庫沒有回傳新的 id");
        } catch (SQLException e) {
            throw failure("新增", e);
        }
    }

    public void execute(String sql) {
        try (Statement statement = connection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw failure("執行 SQL", e);
        }
    }

    public SqlDialect dialect() {
        return dialect;
    }

    @Override
    public void close() {
        closeQuietly();
    }

    static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            int index = i + 1;
            switch (params[i]) {
                case null -> statement.setNull(index, Types.NULL);
                case Money money -> statement.setLong(index, money.toCents());
                case LocalDate date -> statement.setString(index, date.toString());
                case LocalTime time -> statement.setString(index, time.format(TIME_FORMAT));
                case Enum<?> value -> statement.setString(index, value.name());
                case Boolean value -> statement.setBoolean(index, value);
                case Integer value -> statement.setInt(index, value);
                case Long value -> statement.setLong(index, value);
                case String value -> statement.setString(index, value);
                default -> statement.setObject(index, params[i]);
            }
        }
    }

    static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    static LocalDate date(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? null : LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
    }

    static LocalTime time(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? LocalTime.MIDNIGHT : LocalTime.parse(value);
    }

    static Money money(ResultSet resultSet, String column) throws SQLException {
        return Money.ofCents(resultSet.getLong(column));
    }

    private StorageException failure(String action, SQLException e) {
        return new StorageException(dialect.name() + " " + action + "失敗：" + e.getMessage(), e);
    }

    private void rollbackQuietly(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
            // 保留原本的錯誤
        }
    }

    private void closeQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 關閉失敗不影響後續
            }
            connection = null;
        }
    }
}
