package budgetbook.repository;

import java.util.Locale;

public enum StorageType {
    SQLITE("SQLite"),
    MYSQL("MySQL");

    private final String displayName;

    StorageType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static StorageType parse(String text) {
        String value = text == null ? "" : text.strip().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "MYSQL" -> MYSQL;
            case "", "SQLITE" -> SQLITE;
            default -> throw new IllegalArgumentException("Unknown storage type: " + text + " (use sqlite or mysql)");
        };
    }
}
