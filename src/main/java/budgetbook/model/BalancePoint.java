package budgetbook.model;

import java.time.LocalDate;

/** 某一天結束時的總資產，用於餘額趨勢圖。 */
public record BalancePoint(LocalDate date, Money balance) {
}
