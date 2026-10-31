package budgetbook.repository;

import java.util.function.Supplier;

/**
 * 一個資料庫後端提供的全部 repository（Abstract Factory）。
 * SQLite 與 MySQL 都實作這個介面，service 只依賴介面。
 */
public interface Storage extends AutoCloseable {
    WalletRepository wallets();

    CategoryRepository categories();

    TransactionRepository transactions();

    BudgetRepository budgets();

    RecurringRuleRepository recurringRules();

    SavingsGoalRepository savingsGoals();

    SettingsRepository settings();

    /** 在同一個資料庫交易中執行，失敗時全部還原；可以巢狀呼叫。 */
    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    /** 刪除所有記帳資料（保留設定），用於載入示範資料前。 */
    void clearAllData();

    StorageInfo info();

    @Override
    void close();
}
