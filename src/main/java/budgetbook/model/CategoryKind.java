package budgetbook.model;

import budgetbook.exception.ValidationException;

/** 分類屬於支出或收入；轉帳不使用分類。 */
public enum CategoryKind {
    EXPENSE("支出"),
    INCOME("收入");

    private final String displayName;

    CategoryKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public TransactionType toTransactionType() {
        return this == EXPENSE ? TransactionType.EXPENSE : TransactionType.INCOME;
    }

    public static CategoryKind of(TransactionType type) {
        return switch (type) {
            case EXPENSE -> EXPENSE;
            case INCOME -> INCOME;
            case TRANSFER -> throw new ValidationException("分類", "轉帳不需要分類。");
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
