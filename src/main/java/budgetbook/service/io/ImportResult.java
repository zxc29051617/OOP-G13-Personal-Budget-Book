package budgetbook.service.io;

import java.util.List;

/** 匯入結果摘要。 */
public record ImportResult(int imported, int skippedDuplicates, int createdWallets, int createdCategories,
                           List<String> errors) {
    public ImportResult {
        errors = List.copyOf(errors);
    }

    public String summary() {
        StringBuilder text = new StringBuilder("匯入 ").append(imported).append(" 筆交易");
        if (skippedDuplicates > 0) {
            text.append("，略過重複 ").append(skippedDuplicates).append(" 筆");
        }
        if (createdWallets > 0) {
            text.append("，新增錢包 ").append(createdWallets).append(" 個");
        }
        if (createdCategories > 0) {
            text.append("，新增分類 ").append(createdCategories).append(" 個");
        }
        if (!errors.isEmpty()) {
            text.append("，").append(errors.size()).append(" 列格式錯誤未匯入");
        }
        return text.append("。").toString();
    }
}
