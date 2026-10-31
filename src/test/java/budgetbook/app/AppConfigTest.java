package budgetbook.app;

import budgetbook.repository.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {
    @Test
    void defaultsToSqliteInHomeDirectory(@TempDir Path home) {
        AppConfig config = AppConfig.parse(new String[0], Map.of(), home);
        assertEquals(StorageType.SQLITE, config.storage().type());
        assertEquals(home.resolve(".budgetbook/budgetbook.db"), config.storage().sqliteFile());
        assertFalse(config.demo());
    }

    @Test
    void commandLineBeatsEnvironmentBeatsFile(@TempDir Path home) throws IOException {
        Path dataDir = home.resolve(".budgetbook");
        Files.createDirectories(dataDir);
        Files.writeString(dataDir.resolve(AppConfig.CONFIG_FILE),
                "storage=mysql\nmysql.url=jdbc:mysql://db/file\nmysql.user=file\nmysql.password=secret\n");
        AppConfig fromFile = AppConfig.parse(new String[0], Map.of(), home);
        assertEquals(StorageType.MYSQL, fromFile.storage().type());
        assertEquals("jdbc:mysql://db/file", fromFile.storage().jdbcUrl());
        assertFalse(fromFile.storage().toString().contains("secret"));

        AppConfig fromEnv = AppConfig.parse(new String[0], Map.of("BUDGETBOOK_MYSQL_USER", "env"), home);
        assertEquals("env", fromEnv.storage().user());

        AppConfig fromArgs = AppConfig.parse(new String[]{"--storage=sqlite", "--db=" + home.resolve("x.db")},
                Map.of("BUDGETBOOK_STORAGE", "mysql"), home);
        assertEquals(StorageType.SQLITE, fromArgs.storage().type());
        assertEquals(home.resolve("x.db"), fromArgs.storage().sqliteFile());
    }

    @Test
    void flags(@TempDir Path home) {
        AppConfig demo = AppConfig.parse(new String[]{"--demo"}, Map.of(), home);
        assertTrue(demo.demo());
        assertTrue(demo.storage().isInMemory());
        assertTrue(AppConfig.parse(new String[]{"--smoke-test"}, Map.of(), home).smokeTest());
        assertTrue(AppConfig.parse(new String[]{"-h"}, Map.of(), home).help());
        assertThrows(IllegalArgumentException.class, () -> AppConfig.parse(new String[]{"--bogus"}, Map.of(), home));
        assertThrows(IllegalArgumentException.class,
                () -> AppConfig.parse(new String[]{"--storage=oracle"}, Map.of(), home));
    }

    @Test
    void smokeTestRunsHeadless(@TempDir Path dir) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SmokeTest.run(new PrintStream(bytes, true, StandardCharsets.UTF_8), dir);
        String output = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("OK：所有檢查都通過"), output);
        assertFalse(Files.exists(dir.resolve("smoke-test.db")));
    }
}
