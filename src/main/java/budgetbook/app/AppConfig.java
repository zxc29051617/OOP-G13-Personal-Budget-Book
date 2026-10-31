package budgetbook.app;

import budgetbook.repository.StorageConfig;
import budgetbook.repository.StorageType;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 啟動設定。優先順序：命令列參數 &gt; 環境變數 &gt; 設定檔 {@code budgetbook.properties} &gt; 預設值。
 * 預設使用 {@code ~/.budgetbook/budgetbook.db} 這個 SQLite 檔案。
 */
public record AppConfig(StorageConfig storage, Path dataDir, boolean demo, boolean smokeTest, boolean help) {
    public static final String DEFAULT_MYSQL_URL = "jdbc:mysql://localhost:3306/budget_book"
            + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8"
            + "&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true&useSSL=false";
    public static final String CONFIG_FILE = "budgetbook.properties";

    public static final String USAGE = """
            個人記帳本 Personal Budget Book

            用法：java -jar personal-budget-book.jar [選項]

              --demo                 使用示範資料開啟（資料只在記憶體中，不會存檔）
              --db=<檔案>            指定 SQLite 資料庫檔案
              --data-dir=<資料夾>    設定檔與預設資料庫的位置（預設 ~/.budgetbook）
              --storage=mysql        改用 MySQL，連線資訊見 --mysql-url、--mysql-user、--mysql-password
              --smoke-test           不開視窗，自動檢查資料庫與各項功能後結束
              --help                 顯示這段說明

            環境變數：BUDGETBOOK_HOME、BUDGETBOOK_STORAGE、BUDGETBOOK_DB、
                      BUDGETBOOK_MYSQL_URL、BUDGETBOOK_MYSQL_USER、BUDGETBOOK_MYSQL_PASSWORD
            """;

    public static AppConfig parse(String[] args, Map<String, String> env, Path userHome) {
        Map<String, String> options = new HashMap<>();
        boolean demo = false;
        boolean smokeTest = false;
        boolean help = false;
        for (String arg : args) {
            switch (arg) {
                case "--demo" -> demo = true;
                case "--smoke-test" -> smokeTest = true;
                case "--help", "-h" -> help = true;
                default -> {
                    if (!arg.startsWith("--") || !arg.contains("=")) {
                        throw new IllegalArgumentException("無法辨識的參數：" + arg + "\n\n" + USAGE);
                    }
                    int eq = arg.indexOf('=');
                    options.put(arg.substring(2, eq), arg.substring(eq + 1));
                }
            }
        }

        Path dataDir = Path.of(first(options.get("data-dir"), env.get("BUDGETBOOK_HOME"),
                userHome.resolve(".budgetbook").toString()));
        Properties file = loadProperties(dataDir.resolve(CONFIG_FILE));

        StorageType type = StorageType.parse(first(options.get("storage"), env.get("BUDGETBOOK_STORAGE"),
                file.getProperty("storage"), "sqlite"));
        StorageConfig storage;
        if (demo) {
            storage = StorageConfig.inMemory();
        } else if (type == StorageType.MYSQL) {
            storage = StorageConfig.mysql(
                    first(options.get("mysql-url"), env.get("BUDGETBOOK_MYSQL_URL"), file.getProperty("mysql.url"),
                            DEFAULT_MYSQL_URL),
                    first(options.get("mysql-user"), env.get("BUDGETBOOK_MYSQL_USER"), file.getProperty("mysql.user"),
                            "root"),
                    first(options.get("mysql-password"), env.get("BUDGETBOOK_MYSQL_PASSWORD"),
                            file.getProperty("mysql.password"), ""));
        } else {
            storage = StorageConfig.sqlite(Path.of(first(options.get("db"), env.get("BUDGETBOOK_DB"),
                    file.getProperty("sqlite.file"), dataDir.resolve("budgetbook.db").toString())));
        }
        return new AppConfig(storage, dataDir, demo, smokeTest, help);
    }

    private static Properties loadProperties(Path path) {
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new IllegalArgumentException("無法讀取設定檔 " + path + "：" + e.getMessage(), e);
            }
        }
        return properties;
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "";
    }
}
