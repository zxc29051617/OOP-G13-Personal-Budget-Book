package budgetbook.repository.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Strategy：封裝不同資料庫之間的差異（如何連線、建表 SQL 放在哪個資料夾）。
 * Repository 使用的 SQL 只用兩邊都支援的寫法，因此差異都集中在這裡與 migration 檔案。
 */
public interface SqlDialect {
    /** 顯示名稱，例如 SQLite。 */
    String name();

    /** migration SQL 所在的 classpath 資料夾，例如 {@code db/sqlite}。 */
    String migrationFolder();

    Connection openConnection() throws SQLException;

    /** 每次建立連線後要執行的設定，例如 SQLite 要開啟外鍵檢查。 */
    default void configure(Connection connection) throws SQLException {
    }

    /** 記錄 schema 版本的資料表。 */
    String schemaVersionTableSql();

    /** 連線中斷時能不能重新連線；記憶體資料庫重連會遺失資料，所以不行。 */
    default boolean canReconnect() {
        return true;
    }
}
