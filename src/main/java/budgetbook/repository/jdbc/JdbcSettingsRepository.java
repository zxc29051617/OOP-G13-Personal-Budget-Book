package budgetbook.repository.jdbc;

import budgetbook.repository.SettingsRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class JdbcSettingsRepository implements SettingsRepository {
    private final JdbcSession session;

    JdbcSettingsRepository(JdbcSession session) {
        this.session = session;
    }

    @Override
    public Optional<String> get(String key) {
        return session.queryOne("SELECT setting_value FROM settings WHERE setting_key = ?",
                rs -> rs.getString("setting_value"), key);
    }

    @Override
    public void put(String key, String value) {
        // 先更新、沒有資料再新增：兩種資料庫都支援，不必用各自的 upsert 語法
        session.inTransaction(() -> {
            if (session.update("UPDATE settings SET setting_value = ? WHERE setting_key = ?", value, key) == 0) {
                session.update("INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)", key, value);
            }
        });
    }

    @Override
    public void remove(String key) {
        session.update("DELETE FROM settings WHERE setting_key = ?", key);
    }

    @Override
    public Map<String, String> all() {
        Map<String, String> values = new LinkedHashMap<>();
        session.query("SELECT setting_key, setting_value FROM settings ORDER BY setting_key",
                rs -> values.put(rs.getString("setting_key"), rs.getString("setting_value")));
        return values;
    }
}
