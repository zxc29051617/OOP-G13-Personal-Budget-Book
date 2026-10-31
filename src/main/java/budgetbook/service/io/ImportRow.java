package budgetbook.service.io;

import budgetbook.model.Money;
import budgetbook.model.TransactionType;

import java.time.LocalDate;
import java.time.LocalTime;

/** CSV 中解析成功的一列；分類與錢包仍是名稱，匯入時才對應或建立。 */
public record ImportRow(int line, LocalDate date, LocalTime time, TransactionType type, Money amount,
                        String category, String wallet, String toWallet, String note, boolean duplicate) {
    ImportRow markDuplicate() {
        return new ImportRow(line, date, time, type, amount, category, wallet, toWallet, note, true);
    }
}
