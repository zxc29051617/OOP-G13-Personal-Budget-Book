package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * 一筆交易。不可變；修改時以 {@link #toBuilder()} 建立新物件。
 *
 * <ul>
 *   <li>支出／收入：必須有 {@code categoryId}，不能有 {@code toWalletId}</li>
 *   <li>轉帳：從 {@code walletId} 轉到 {@code toWalletId}，不使用分類</li>
 * </ul>
 *
 * @param recurringRuleId 由定期交易自動產生時記錄來源規則，否則為 null
 */
public record Transaction(long id, TransactionType type, LocalDate date, LocalTime time, Money amount,
                          Long categoryId, long walletId, Long toWalletId, String note, Long recurringRuleId)
        implements Entity {
    public static final int MAX_NOTE_LENGTH = 200;

    public Transaction {
        Checks.requireValue(type, "交易類型");
        Checks.requireValue(date, "日期");
        time = time == null ? LocalTime.MIDNIGHT : time.truncatedTo(ChronoUnit.MINUTES);
        Checks.requirePositive(amount, "金額");
        if (type == TransactionType.TRANSFER) {
            categoryId = null;
        } else {
            toWalletId = null;
        }
        validateParties(type, categoryId, walletId, toWalletId);
        note = Checks.optionalText(note, "備註", MAX_NOTE_LENGTH);
    }

    /** 交易與定期交易共用的規則：錢包必填；收支要有分類；轉帳的兩個錢包要不同。 */
    static void validateParties(TransactionType type, Long categoryId, long walletId, Long toWalletId) {
        if (walletId <= 0) {
            throw new ValidationException("錢包", "請選擇錢包。");
        }
        if (type == TransactionType.TRANSFER) {
            if (toWalletId == null || toWalletId <= 0) {
                throw new ValidationException("轉入錢包", "請選擇要轉入的錢包。");
            }
            if (toWalletId == walletId) {
                throw new ValidationException("轉入錢包", "轉出與轉入的錢包不能相同。");
            }
        } else if (categoryId == null || categoryId <= 0) {
            throw new ValidationException("分類", "請選擇分類。");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id).type(type).date(date).time(time).amount(amount)
                .categoryId(categoryId).walletId(walletId).toWalletId(toWalletId)
                .note(note).recurringRuleId(recurringRuleId);
    }

    public Transaction withId(long newId) {
        return toBuilder().id(newId).build();
    }

    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    public boolean isTransfer() {
        return type == TransactionType.TRANSFER;
    }

    public boolean involvesWallet(long wallet) {
        return walletId == wallet || (toWalletId != null && toWalletId == wallet);
    }

    /** 這筆交易讓指定錢包的餘額增加（正）或減少（負）多少。 */
    public Money balanceEffectOn(long wallet) {
        return switch (type) {
            case INCOME -> walletId == wallet ? amount : Money.ZERO;
            case EXPENSE -> walletId == wallet ? amount.negate() : Money.ZERO;
            case TRANSFER -> {
                Money effect = Money.ZERO;
                if (walletId == wallet) {
                    effect = effect.minus(amount);
                }
                if (toWalletId != null && toWalletId == wallet) {
                    effect = effect.plus(amount);
                }
                yield effect;
            }
        };
    }

    /** 對「所有錢包總額」的影響：收入為正、支出為負、轉帳為 0。 */
    public Money netWorthEffect() {
        return switch (type) {
            case INCOME -> amount;
            case EXPENSE -> amount.negate();
            case TRANSFER -> Money.ZERO;
        };
    }

    public LocalDateTime dateTime() {
        return date.atTime(time);
    }

    public YearMonth yearMonth() {
        return YearMonth.from(date);
    }

    /** Builder 模式：交易欄位多，用具名的方法組裝比十個參數的建構子清楚。 */
    public static final class Builder {
        private long id;
        private TransactionType type = TransactionType.EXPENSE;
        private LocalDate date;
        private LocalTime time = LocalTime.MIDNIGHT;
        private Money amount;
        private Long categoryId;
        private long walletId;
        private Long toWalletId;
        private String note = "";
        private Long recurringRuleId;

        private Builder() {
        }

        public Builder id(long value) {
            id = value;
            return this;
        }

        public Builder type(TransactionType value) {
            type = value;
            return this;
        }

        public Builder expense() {
            return type(TransactionType.EXPENSE);
        }

        public Builder income() {
            return type(TransactionType.INCOME);
        }

        public Builder transfer(long fromWallet, long toWallet) {
            type = TransactionType.TRANSFER;
            walletId = fromWallet;
            toWalletId = toWallet;
            return this;
        }

        public Builder date(LocalDate value) {
            date = value;
            return this;
        }

        public Builder time(LocalTime value) {
            time = value;
            return this;
        }

        public Builder amount(Money value) {
            amount = value;
            return this;
        }

        public Builder amount(long value) {
            return amount(Money.of(value));
        }

        public Builder categoryId(Long value) {
            categoryId = value;
            return this;
        }

        public Builder walletId(long value) {
            walletId = value;
            return this;
        }

        public Builder toWalletId(Long value) {
            toWalletId = value;
            return this;
        }

        public Builder note(String value) {
            note = value;
            return this;
        }

        public Builder recurringRuleId(Long value) {
            recurringRuleId = value;
            return this;
        }

        public Transaction build() {
            return new Transaction(id, type, date, time, amount, categoryId, walletId, toWalletId, note,
                    recurringRuleId);
        }
    }
}
