package budgetbook.repository;

/** 給設定頁顯示的資料庫資訊。 */
public record StorageInfo(String backend, String location, int schemaVersion) {
}
