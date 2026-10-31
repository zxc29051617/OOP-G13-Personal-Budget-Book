package budgetbook.repository.jdbc;

import budgetbook.repository.BudgetRepository;
import budgetbook.repository.CategoryRepository;
import budgetbook.repository.RecurringRuleRepository;
import budgetbook.repository.SavingsGoalRepository;
import budgetbook.repository.SettingsRepository;
import budgetbook.repository.Storage;
import budgetbook.repository.StorageInfo;
import budgetbook.repository.TransactionRepository;
import budgetbook.repository.WalletRepository;

import java.util.List;
import java.util.function.Supplier;

/** 以 JDBC 實作的 {@link Storage}；SQLite 與 MySQL 共用，差異由 {@link SqlDialect} 處理。 */
public final class JdbcStorage implements Storage {
    /** 刪除順序：先刪會參照別人的資料表。 */
    private static final List<String> DATA_TABLES = List.of(
            "transactions", "recurring_rules", "budgets", "savings_goals", "categories", "wallets");

    private final JdbcSession session;
    private final SqlDialect dialect;
    private final String location;
    private final WalletRepository wallets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final RecurringRuleRepository recurringRules;
    private final SavingsGoalRepository savingsGoals;
    private final SettingsRepository settings;

    public JdbcStorage(JdbcSession session, SqlDialect dialect, String location) {
        this.session = session;
        this.dialect = dialect;
        this.location = location;
        this.wallets = new JdbcWalletRepository(session);
        this.categories = new JdbcCategoryRepository(session);
        this.transactions = new JdbcTransactionRepository(session);
        this.budgets = new JdbcBudgetRepository(session);
        this.recurringRules = new JdbcRecurringRuleRepository(session);
        this.savingsGoals = new JdbcSavingsGoalRepository(session);
        this.settings = new JdbcSettingsRepository(session);
    }

    public int migrate() {
        return new SchemaMigrator(session).migrate();
    }

    @Override
    public WalletRepository wallets() {
        return wallets;
    }

    @Override
    public CategoryRepository categories() {
        return categories;
    }

    @Override
    public TransactionRepository transactions() {
        return transactions;
    }

    @Override
    public BudgetRepository budgets() {
        return budgets;
    }

    @Override
    public RecurringRuleRepository recurringRules() {
        return recurringRules;
    }

    @Override
    public SavingsGoalRepository savingsGoals() {
        return savingsGoals;
    }

    @Override
    public SettingsRepository settings() {
        return settings;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return session.inTransaction(work);
    }

    @Override
    public void clearAllData() {
        session.inTransaction(() -> DATA_TABLES.forEach(table -> session.update("DELETE FROM " + table)));
    }

    @Override
    public StorageInfo info() {
        return new StorageInfo(dialect.name(), location, new SchemaMigrator(session).currentVersion());
    }

    @Override
    public void close() {
        session.close();
    }
}
