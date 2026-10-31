package budgetbook.repository;

import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionRepository extends Repository<Transaction> {
    /** 依條件查詢，結果由新到舊排序。 */
    List<Transaction> find(TransactionQuery query);

    List<Transaction> findBetween(DateRange range);

    /** 最新的幾筆交易。 */
    List<Transaction> findRecent(int limit);

    /**
     * 每個錢包因交易而增減的總額（不含初始金額），以資料庫的 SUM 計算。
     *
     * @param upTo 只計算這一天（含）以前的交易；null 代表全部
     */
    Map<Long, Money> netChangeByWallet(LocalDate upTo);

    /** 轉出或轉入是這個錢包的交易筆數。 */
    long countByWallet(long walletId);

    long countByCategory(long categoryId);

    Optional<LocalDate> earliestDate();

    /** 最近使用過的不重複備註，用於輸入時的自動完成。 */
    List<String> recentNotes(int limit);
}
