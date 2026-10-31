package budgetbook.repository.jdbc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** 內建 SQLite：資料存在單一檔案，不需要安裝資料庫伺服器。file 為 null 時使用記憶體資料庫。 */
public final class SqliteDialect implements SqlDialect {
    private final Path file;

    public SqliteDialect(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "SQLite";
    }

    @Override
    public String migrationFolder() {
        return "db/sqlite";
    }

    @Override
    public Connection openConnection() throws SQLException {
        if (file == null) {
            return DriverManager.getConnection("jdbc:sqlite::memory:");
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("無法建立資料夾：" + file.getParent(), e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
    }

    @Override
    public void configure(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
    }

    @Override
    public String schemaVersionTableSql() {
        return """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version     INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at  TEXT NOT NULL
                )""";
    }

    @Override
    public boolean canReconnect() {
        return file != null;
    }
}
