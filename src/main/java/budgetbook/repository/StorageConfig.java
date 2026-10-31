package budgetbook.repository;

import java.nio.file.Path;

/**
 * 資料庫設定。SQLite 只需要檔案路徑（null 代表記憶體資料庫）；MySQL 需要 JDBC URL 與帳號密碼。
 * {@link #toString()} 不會印出密碼。
 */
public record StorageConfig(StorageType type, Path sqliteFile, String jdbcUrl, String user, String password) {
    public static StorageConfig sqlite(Path file) {
        return new StorageConfig(StorageType.SQLITE, file, null, null, null);
    }

    /** 不寫入檔案的 SQLite 資料庫，用於測試與示範模式。 */
    public static StorageConfig inMemory() {
        return new StorageConfig(StorageType.SQLITE, null, null, null, null);
    }

    public static StorageConfig mysql(String jdbcUrl, String user, String password) {
        return new StorageConfig(StorageType.MYSQL, null, jdbcUrl, user, password == null ? "" : password);
    }

    public boolean isInMemory() {
        return type == StorageType.SQLITE && sqliteFile == null;
    }

    public String describe() {
        if (type == StorageType.MYSQL) {
            int query = jdbcUrl.indexOf('?');
            return query < 0 ? jdbcUrl : jdbcUrl.substring(0, query);
        }
        return isInMemory() ? "記憶體（不存檔）" : sqliteFile.toAbsolutePath().normalize().toString();
    }

    @Override
    public String toString() {
        return type.displayName() + " " + describe();
    }
}
