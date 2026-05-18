package persistence;

import model.Account;
import model.BudgetSettings;
import model.Transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class BudgetBookStore {
    private static final int SETTINGS_ID = 1;

    private final Path dataDir;
    private final Path accountsFile;
    private final Path transactionsFile;
    private final Path settingsFile;

    private final List<Account> accounts = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private BudgetSettings settings = new BudgetSettings(10000, 0);
    private DatabaseConfig config;

    public BudgetBookStore(Path dataDir) {
        this.dataDir = dataDir;
        this.accountsFile = dataDir.resolve("accounts.csv");
        this.transactionsFile = dataDir.resolve("transactions.csv");
        this.settingsFile = dataDir.resolve("settings.csv");
    }

    public void load() throws IOException {
        Files.createDirectories(dataDir);
        config = DatabaseConfig.load(dataDir);
        try {
            ensureSchema();
            reloadFromDatabase();
            if (accounts.isEmpty() && hasLegacyCsvData()) {
                importLegacyCsvData();
                reloadFromDatabase();
            }
            if (accounts.isEmpty()) {
                addAccount("現金", "現金", 3000);
                addAccount("銀行帳戶", "銀行", 15000);
                addAccount("電子支付", "數位支付", 1000);
            }
        } catch (SQLException e) {
            throw asIOException("Load MySQL data", e);
        }
    }

    public void save() throws IOException {
        try {
            saveSettings();
        } catch (SQLException e) {
            throw asIOException("Save MySQL data", e);
        }
    }

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public List<Transaction> getTransactions() {
        List<Transaction> copy = new ArrayList<>(transactions);
        copy.sort(Comparator.comparing(Transaction::getDate)
                .thenComparing(Transaction::getTime)
                .thenComparing(Transaction::getId)
                .reversed());
        return copy;
    }

    public BudgetSettings getSettings() {
        return settings;
    }

    public Account addAccount(String name, String type, double initialBalance) {
        String sql = "INSERT INTO accounts (name, type, initial_balance) VALUES (?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, type);
            statement.setDouble(3, initialBalance);
            statement.executeUpdate();

            Account account = new Account(generatedId(statement), name, type, initialBalance);
            accounts.add(account);
            return account;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to add account: " + e.getMessage(), e);
        }
    }

    public void deleteAccount(int accountId) {
        boolean used = transactions.stream().anyMatch(t -> t.getAccountId() == accountId);
        if (used) {
            throw new IllegalArgumentException("這個錢包已有交易紀錄，不能刪除。");
        }

        String sql = "DELETE FROM accounts WHERE id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            statement.executeUpdate();
            accounts.removeIf(a -> a.getId() == accountId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete account: " + e.getMessage(), e);
        }
    }

    public Transaction addTransaction(LocalDate date, Transaction.Kind kind, String category, int accountId, double amount, String note) {
        return addTransaction(date, LocalTime.now().withSecond(0).withNano(0), kind, category, accountId, amount, note);
    }

    public Transaction addTransaction(LocalDate date, LocalTime time, Transaction.Kind kind, String category, int accountId, double amount, String note) {
        String sql = """
                INSERT INTO transactions (date, transaction_time, kind, category, account_id, amount, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        LocalTime safeTime = time == null ? LocalTime.MIDNIGHT : time;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDate(1, Date.valueOf(date));
            statement.setTime(2, Time.valueOf(safeTime));
            statement.setString(3, kind.name());
            statement.setString(4, category);
            statement.setInt(5, accountId);
            statement.setDouble(6, amount);
            statement.setString(7, note);
            statement.executeUpdate();

            int id = generatedId(statement);
            Transaction transaction = new Transaction(id, date, safeTime, kind, category, accountId, amount, note);
            transactions.add(transaction);
            return transaction;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to add transaction: " + e.getMessage(), e);
        }
    }

    public void deleteTransaction(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            transactions.removeIf(t -> t.getId() == id);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete transaction: " + e.getMessage(), e);
        }
    }

    public Account findAccount(int id) {
        for (Account account : accounts) {
            if (account.getId() == id) {
                return account;
            }
        }
        return null;
    }

    public double getAccountBalance(int accountId) {
        Account account = findAccount(accountId);
        double balance = account == null ? 0 : account.getInitialBalance();
        for (Transaction transaction : transactions) {
            if (transaction.getAccountId() == accountId) {
                balance += transaction.getKind() == Transaction.Kind.INCOME
                        ? transaction.getAmount()
                        : -transaction.getAmount();
            }
        }
        return balance;
    }

    private void ensureSchema() throws SQLException {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL,
                        type VARCHAR(100) NOT NULL,
                        initial_balance DOUBLE NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        date DATE NOT NULL,
                        transaction_time TIME NOT NULL DEFAULT '00:00:00',
                        kind VARCHAR(20) NOT NULL,
                        category VARCHAR(100) NOT NULL,
                        account_id INT NOT NULL,
                        amount DOUBLE NOT NULL,
                        note VARCHAR(255),
                        CONSTRAINT fk_transactions_account
                            FOREIGN KEY (account_id) REFERENCES accounts(id)
                            ON DELETE RESTRICT
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            ensureTransactionTimeColumn(connection, statement);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS settings (
                        id INT PRIMARY KEY,
                        monthly_limit DOUBLE NOT NULL,
                        points INT NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }
        ensureDefaultSettingsRow();
    }

    private void ensureTransactionTimeColumn(Connection connection, Statement statement) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "transactions", "transaction_time")) {
            if (!columns.next()) {
                statement.executeUpdate("ALTER TABLE transactions ADD COLUMN transaction_time TIME NOT NULL DEFAULT '00:00:00' AFTER date");
            }
        }
    }

    private void ensureDefaultSettingsRow() throws SQLException {
        String sql = """
                INSERT INTO settings (id, monthly_limit, points)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE id = id
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, SETTINGS_ID);
            statement.setDouble(2, 10000);
            statement.setInt(3, 0);
            statement.executeUpdate();
        }
    }

    private void reloadFromDatabase() throws SQLException {
        loadAccountsFromDatabase();
        loadTransactionsFromDatabase();
        loadSettingsFromDatabase();
    }

    private void loadAccountsFromDatabase() throws SQLException {
        accounts.clear();
        String sql = "SELECT id, name, type, initial_balance FROM accounts ORDER BY id";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                accounts.add(new Account(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getString("type"),
                        resultSet.getDouble("initial_balance")));
            }
        }
    }

    private void loadTransactionsFromDatabase() throws SQLException {
        transactions.clear();
        String sql = """
                SELECT id, date, transaction_time, kind, category, account_id, amount, note
                FROM transactions
                ORDER BY date DESC, transaction_time DESC, id DESC
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Time transactionTime = resultSet.getTime("transaction_time");
                transactions.add(new Transaction(
                        resultSet.getInt("id"),
                        resultSet.getDate("date").toLocalDate(),
                        transactionTime == null ? LocalTime.MIDNIGHT : transactionTime.toLocalTime(),
                        Transaction.Kind.valueOf(resultSet.getString("kind")),
                        resultSet.getString("category"),
                        resultSet.getInt("account_id"),
                        resultSet.getDouble("amount"),
                        resultSet.getString("note")));
            }
        }
    }

    private void loadSettingsFromDatabase() throws SQLException {
        String sql = "SELECT monthly_limit, points FROM settings WHERE id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, SETTINGS_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    settings = new BudgetSettings(
                            resultSet.getDouble("monthly_limit"),
                            resultSet.getInt("points"));
                }
            }
        }
    }

    private void saveSettings() throws SQLException {
        try (Connection connection = connect()) {
            saveSettings(connection);
        }
    }

    private void saveSettings(Connection connection) throws SQLException {
        String update = "UPDATE settings SET monthly_limit = ?, points = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setDouble(1, settings.getMonthlyLimit());
            statement.setInt(2, settings.getPoints());
            statement.setInt(3, SETTINGS_ID);
            if (statement.executeUpdate() > 0) {
                return;
            }
        }

        String insert = "INSERT INTO settings (id, monthly_limit, points) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setInt(1, SETTINGS_ID);
            statement.setDouble(2, settings.getMonthlyLimit());
            statement.setInt(3, settings.getPoints());
            statement.executeUpdate();
        }
    }

    private boolean hasLegacyCsvData() {
        return Files.exists(accountsFile) || Files.exists(transactionsFile) || Files.exists(settingsFile);
    }

    private void importLegacyCsvData() throws IOException, SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                importLegacyAccounts(connection);
                importLegacySettings(connection);
                importLegacyTransactions(connection);
                connection.commit();
            } catch (IOException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            }
        }
    }

    private void importLegacyAccounts(Connection connection) throws IOException, SQLException {
        if (!Files.exists(accountsFile)) {
            return;
        }
        String sql = "INSERT INTO accounts (id, name, type, initial_balance) VALUES (?, ?, ?, ?)";
        try (BufferedReader reader = Files.newBufferedReader(accountsFile, StandardCharsets.UTF_8);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> row = CsvUtil.parse(line);
                if (row.size() >= 4) {
                    statement.setInt(1, Integer.parseInt(row.get(0)));
                    statement.setString(2, row.get(1));
                    statement.setString(3, row.get(2));
                    statement.setDouble(4, Double.parseDouble(row.get(3)));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private void importLegacyTransactions(Connection connection) throws IOException, SQLException {
        if (!Files.exists(transactionsFile)) {
            return;
        }
        String sql = """
                INSERT INTO transactions (id, date, transaction_time, kind, category, account_id, amount, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (BufferedReader reader = Files.newBufferedReader(transactionsFile, StandardCharsets.UTF_8);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> row = CsvUtil.parse(line);
                if (row.size() >= 7) {
                    statement.setInt(1, Integer.parseInt(row.get(0)));
                    statement.setDate(2, Date.valueOf(LocalDate.parse(row.get(1))));
                    statement.setTime(3, Time.valueOf(LocalTime.MIDNIGHT));
                    statement.setString(4, row.get(2));
                    statement.setString(5, row.get(3));
                    statement.setInt(6, Integer.parseInt(row.get(4)));
                    statement.setDouble(7, Double.parseDouble(row.get(5)));
                    statement.setString(8, row.get(6));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private void importLegacySettings(Connection connection) throws IOException, SQLException {
        if (!Files.exists(settingsFile)) {
            return;
        }
        List<String> lines = Files.readAllLines(settingsFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return;
        }
        List<String> row = CsvUtil.parse(lines.get(0));
        if (row.size() >= 2) {
            settings = new BudgetSettings(Double.parseDouble(row.get(0)), Integer.parseInt(row.get(1)));
            saveSettings(connection);
        }
    }

    private Connection connect() throws SQLException {
        if (config == null) {
            config = DatabaseConfig.defaults();
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J not found. Put mysql-connector-j-*.jar in the lib folder.", e);
        }
        return DriverManager.getConnection(config.url, config.user, config.password);
    }

    private int generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("Database did not return a generated id.");
    }

    private IOException asIOException(String action, SQLException e) {
        return new IOException(action + " failed: " + e.getMessage(), e);
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original database error.
        }
    }

    private static class DatabaseConfig {
        private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/personal_budget_book"
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Taipei"
                + "&createDatabaseIfNotExist=true"
                + "&useUnicode=true"
                + "&characterEncoding=utf8";
        private static final String DEFAULT_USER = "root";
        private static final String DEFAULT_PASSWORD = "";

        private final String url;
        private final String user;
        private final String password;

        private DatabaseConfig(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        private static DatabaseConfig load(Path dataDir) throws IOException {
            Properties properties = new Properties();
            Path configFile = dataDir.resolve("database.properties");
            if (Files.exists(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }

            return new DatabaseConfig(
                    firstValue(System.getenv("PBB_DB_URL"), properties.getProperty("url"), DEFAULT_URL),
                    firstValue(System.getenv("PBB_DB_USER"), properties.getProperty("user"), DEFAULT_USER),
                    firstValue(System.getenv("PBB_DB_PASSWORD"), properties.getProperty("password"), DEFAULT_PASSWORD));
        }

        private static DatabaseConfig defaults() {
            return new DatabaseConfig(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
        }

        private static String firstValue(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
            return "";
        }
    }
}
