package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.util.Locale;

/** 交易類型。轉帳只在錢包之間移動金額，不算收入也不算支出。 */
public enum TransactionType {
    EXPENSE("支出"),
    INCOME("收入"),
    TRANSFER("轉帳");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** 接受英文名稱（不分大小寫）或中文名稱，例如 {@code EXPENSE}、{@code expense}、{@code 支出}。 */
    public static TransactionType parse(String text) {
        String value = text == null ? "" : text.strip();
        for (TransactionType type : values()) {
            if (type.name().equals(value.toUpperCase(Locale.ROOT)) || type.displayName.equals(value)) {
                return type;
            }
        }
        throw new ValidationException("類型", "無法辨識的交易類型：" + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
