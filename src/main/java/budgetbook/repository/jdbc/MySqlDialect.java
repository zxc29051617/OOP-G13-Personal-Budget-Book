package budgetbook.repository.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** 選用的 MySQL 後端（原版專題使用的資料庫）。 */
public final class MySqlDialect implements SqlDialect {
    private final String url;
    private final String user;
    private final String password;

    public MySqlDialect(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public String name() {
        return "MySQL";
    }

    @Override
    public String migrationFolder() {
        return "db/mysql";
    }

    @Override
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public String schemaVersionTableSql() {
        return """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version     INT          NOT NULL PRIMARY KEY,
                    description VARCHAR(200) NOT NULL,
                    applied_at  VARCHAR(30)  NOT NULL
                ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci""";
    }
}
