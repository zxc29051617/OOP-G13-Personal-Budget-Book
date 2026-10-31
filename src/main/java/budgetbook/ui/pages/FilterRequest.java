package budgetbook.ui.pages;

import budgetbook.model.Category;
import budgetbook.model.Wallet;

/** 其他頁面要求交易頁只顯示某個分類或錢包的事件。 */
public record FilterRequest(Category category, Wallet wallet) {
}
