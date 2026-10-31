package budgetbook.model;

/**
 * 交易查詢條件（Query Object）。每個欄位為 null 代表不限制；{@code text} 會比對備註、分類名稱與錢包名稱。
 *
 * @param walletId 轉出或轉入其中一方是這個錢包就算符合
 */
public record TransactionQuery(DateRange range, TransactionType type, Long categoryId, Long walletId, String text) {
    public TransactionQuery {
        text = text == null ? "" : text.strip();
    }

    public static TransactionQuery all() {
        return new TransactionQuery(null, null, null, null, "");
    }

    public TransactionQuery withRange(DateRange value) {
        return new TransactionQuery(value, type, categoryId, walletId, text);
    }

    public TransactionQuery withType(TransactionType value) {
        return new TransactionQuery(range, value, categoryId, walletId, text);
    }

    public TransactionQuery withCategory(Long value) {
        return new TransactionQuery(range, type, value, walletId, text);
    }

    public TransactionQuery withWallet(Long value) {
        return new TransactionQuery(range, type, categoryId, value, text);
    }

    public TransactionQuery withText(String value) {
        return new TransactionQuery(range, type, categoryId, walletId, value);
    }

    public boolean hasText() {
        return !text.isEmpty();
    }
}
