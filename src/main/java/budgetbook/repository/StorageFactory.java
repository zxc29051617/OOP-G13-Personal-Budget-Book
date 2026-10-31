package budgetbook.repository;

import budgetbook.repository.jdbc.JdbcSession;
import budgetbook.repository.jdbc.JdbcStorage;
import budgetbook.repository.jdbc.MySqlDialect;
import budgetbook.repository.jdbc.SqlDialect;
import budgetbook.repository.jdbc.SqliteDialect;

/**
 * Factory：依設定建立對應的資料庫後端，並自動執行 schema 升級。
 * 新增後端時只要多一個 {@link SqlDialect} 與 migration SQL，service 與 UI 都不用改。
 */
public final class StorageFactory {
    private StorageFactory() {
    }

    public static Storage open(StorageConfig config) {
        SqlDialect dialect = switch (config.type()) {
            case SQLITE -> new SqliteDialect(config.sqliteFile());
            case MYSQL -> new MySqlDialect(config.jdbcUrl(), config.user(), config.password());
        };
        JdbcStorage storage = new JdbcStorage(new JdbcSession(dialect), dialect, config.describe());
        try {
            storage.migrate();
        } catch (RuntimeException e) {
            storage.close();
            throw e;
        }
        return storage;
    }
}
