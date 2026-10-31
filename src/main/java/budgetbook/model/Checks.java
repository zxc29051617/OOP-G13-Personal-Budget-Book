package budgetbook.model;

import budgetbook.exception.ValidationException;

/** 共用的輸入檢查，失敗時丟出帶有中文訊息的 {@link ValidationException}。 */
public final class Checks {
    private Checks() {
    }

    /** 去掉前後空白後不可為空，且長度（以字元計）不可超過上限。 */
    public static String requireText(String value, String field, int maxLength) {
        String text = value == null ? "" : value.strip();
        if (text.isEmpty()) {
            throw new ValidationException(field, field + "不能空白。");
        }
        return requireMaxLength(text, field, maxLength);
    }

    /** 可以空白；null 會變成空字串。 */
    public static String optionalText(String value, String field, int maxLength) {
        String text = value == null ? "" : value.strip();
        return requireMaxLength(text, field, maxLength);
    }

    public static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new ValidationException(field, "請選擇" + field + "。");
        }
        return value;
    }

    public static Money requirePositive(Money amount, String field) {
        requireValue(amount, field);
        if (!amount.isPositive()) {
            throw new ValidationException(field, field + "必須大於 0。");
        }
        return amount;
    }

    public static Money requireNotNegative(Money amount, String field) {
        requireValue(amount, field);
        if (amount.isNegative()) {
            throw new ValidationException(field, field + "不能是負數。");
        }
        return amount;
    }

    public static int requireRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new ValidationException(field, field + "必須介於 " + min + " 到 " + max + " 之間。");
        }
        return value;
    }

    private static String requireMaxLength(String text, String field, int maxLength) {
        if (text.codePointCount(0, text.length()) > maxLength) {
            throw new ValidationException(field, field + "最多 " + maxLength + " 個字。");
        }
        return text;
    }
}
