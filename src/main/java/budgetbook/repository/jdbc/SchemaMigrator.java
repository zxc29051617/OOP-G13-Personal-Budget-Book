package budgetbook.repository.jdbc;

import budgetbook.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 簡單的 schema 版本管理（概念同 Flyway）：資料庫記錄目前版本，
 * 啟動時依序執行尚未套用的 {@code V<版本>__<名稱>.sql}。
 */
public final class SchemaMigrator {
    /** 依版本排序；新增資料表或欄位時，在最後加一個版本並撰寫兩種資料庫的 SQL。 */
    public static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, "core_tables", "錢包、分類、交易與設定"),
            new Migration(2, "planning", "預算、定期交易與儲蓄目標"));

    private final JdbcSession session;

    public SchemaMigrator(JdbcSession session) {
        this.session = session;
    }

    public record Migration(int version, String name, String description) {
        String resource(String folder) {
            return folder + "/V" + version + "__" + name + ".sql";
        }
    }

    /** 套用所有尚未執行的 migration，回傳最新版本號。 */
    public int migrate() {
        session.execute(session.dialect().schemaVersionTableSql());
        int current = currentVersion();
        for (Migration migration : MIGRATIONS) {
            if (migration.version() > current) {
                apply(migration);
                current = migration.version();
            }
        }
        return current;
    }

    public int currentVersion() {
        return (int) session.queryLong("SELECT COALESCE(MAX(version), 0) FROM schema_version");
    }

    public static int latestVersion() {
        return MIGRATIONS.get(MIGRATIONS.size() - 1).version();
    }

    private void apply(Migration migration) {
        String sql = readResource(migration.resource(session.dialect().migrationFolder()));
        session.inTransaction(() -> {
            for (String statement : splitStatements(sql)) {
                session.execute(statement);
            }
            session.update("INSERT INTO schema_version (version, description, applied_at) VALUES (?, ?, ?)",
                    migration.version(), migration.description(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        });
    }

    private static String readResource(String path) {
        try (InputStream in = SchemaMigrator.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new StorageException("找不到資料庫升級檔：" + path, null);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("無法讀取資料庫升級檔：" + path, e);
        }
    }

    /** 以分號切開 SQL，並去掉 {@code --} 註解；migration 檔中不使用含分號的字串。 */
    static List<String> splitStatements(String sql) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\\R")) {
            int comment = line.indexOf("--");
            cleaned.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        List<String> statements = new ArrayList<>();
        for (String part : cleaned.toString().split(";")) {
            if (!part.isBlank()) {
                statements.add(part.strip());
            }
        }
        return statements;
    }
}
