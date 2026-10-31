package budgetbook.repository;

import java.util.Map;
import java.util.Optional;

/** 簡單的 key-value 設定，例如主題、種樹目標。 */
public interface SettingsRepository {
    Optional<String> get(String key);

    void put(String key, String value);

    void remove(String key);

    Map<String, String> all();
}
