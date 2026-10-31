# 設計模式

下表列出程式中使用的設計模式、解決的問題與所在位置，後面各節附上關鍵程式片段。

| 模式 | 解決的問題 | 位置 |
|---|---|---|
| Repository | service 不需要知道資料存在哪裡、怎麼查詢 | `repository/Repository.java` 與各個 `*Repository` 介面、`repository/jdbc/Jdbc*Repository.java` |
| Abstract Factory／Factory | 依設定建立整組資料庫物件；建立外觀一致的圖表 | `repository/Storage.java`、`repository/StorageFactory.java`、`ui/components/Charts.java` |
| Strategy | SQLite 與 MySQL 的差異；月報與年報的差異；報表輸出格式 | `repository/jdbc/SqlDialect.java`、`service/report/ReportPeriod.java`、`service/report/ReportExporter.java` |
| Template Method | 所有資料表共用的 CRUD 流程；所有頁面共用的重新整理流程 | `repository/jdbc/JdbcRepository.java`、`ui/Page.java` |
| Command | 每個修改都可以復原與重做；選單、按鈕與快捷鍵共用同一個動作 | `command/`、`ui/AppActions.java` |
| Composite | 一次修改多筆資料的操作當成一個指令 | `command/CompositeCommand.java` |
| Observer | 資料改變後通知所有相關畫面 | `event/EventBus.java`、`event/DataChangedEvent.java`、`ui/AppShell.java` |
| Chain of Responsibility | 查詢助手依序判斷由哪個規則回答問題 | `service/assistant/QueryHandler.java`、`service/assistant/QueryAssistantService.java` |
| Builder | 交易欄位多，用具名的方法組裝 | `model/Transaction.java`（`Transaction.Builder`） |
| Value Object | 金額、日期區間等以值比較、不可修改的物件 | `model/Money.java`、`model/DateRange.java`、所有 `record` |
| Query Object | 交易查詢條件包成一個物件，再轉成 SQL | `model/TransactionQuery.java`、`repository/jdbc/JdbcTransactionRepository.java` |
| 依賴注入與組合根 | 物件的建立集中在一處，其餘類別只透過建構子取得相依物件 | `service/AppContext.java` |

## Repository

```java
public interface Repository<T extends Entity> {
    Optional<T> findById(long id);
    List<T> findAll();
    T save(T entity);      // id 為 0 時新增，否則更新
    T restore(T entity);   // 以原本的 id 寫回（復原時使用）
    void deleteById(long id);
}
```

`TransactionService` 只依賴 `TransactionRepository` 介面。單元測試使用記憶體中的 SQLite；
`MySqlStorageTest` 對真正的 MySQL 執行同樣的流程，service 的程式完全不用改。

## Strategy：資料庫方言

```java
public interface SqlDialect {
    String name();
    String migrationFolder();          // db/sqlite 或 db/mysql
    Connection openConnection() throws SQLException;
    default void configure(Connection connection) throws SQLException { }
    String schemaVersionTableSql();
}
```

`SqliteDialect` 在連線後開啟外鍵檢查（`PRAGMA foreign_keys = ON`），`MySqlDialect` 使用帳號密碼連線。
Repository 中的 SQL 只使用兩邊都支援的語法（例如設定值以「先 UPDATE、沒有資料再 INSERT」取代各自的 upsert，
`LIKE` 以 `!` 作為跳脫字元），差異集中在方言物件與兩份建表 SQL。

## Strategy：報表

`ReportService.generate(ReportPeriod)` 用同一套流程產生報表；月報與年報的差別由策略物件決定：

| | `MonthlyPeriod` | `YearlyPeriod` |
|---|---|---|
| 期間 | 一個月 | 一整年 |
| 上一期 | 上個月 | 去年 |
| 時間序列 | 每日支出 | 每月收入與支出 |
| 預算比較 | 有 | 沒有 |

輸出格式是另一組策略：`CsvReportExporter` 與 `HtmlReportExporter` 都實作 `ReportExporter`，報表頁的兩個匯出按鈕只是換一個物件。

## Template Method

`JdbcRepository<T>` 寫好新增、更新、刪除、查詢的流程，子類別只提供資料表的細節：

```java
final class JdbcWalletRepository extends JdbcRepository<Wallet> implements WalletRepository {
    JdbcWalletRepository(JdbcSession session) {
        super(session, "wallets", "錢包", "name", "type", "initial_cents", "color", "archived");
    }

    @Override
    protected Wallet map(ResultSet rs) throws SQLException { ... }   // 一列資料 → 物件

    @Override
    protected Object[] values(Wallet w) { ... }                      // 物件 → 欄位值
}
```

`ui/Page.java` 也是同樣的做法：`refresh()` 固定呼叫子類別的 `reload()`，再處理過期標記與重繪。

## Command 與 Composite

```java
public interface Command {
    void execute();
    void undo();
    default void redo() { execute(); }
    String description();     // 顯示在狀態列與「編輯」選單，例如「新增支出 餐飲 $120」
    Set<Topic> topics();      // 影響哪些資料，用於通知畫面
}
```

- `SaveCommand`：新增時記下新 id，復原時刪除，重做時以同一個 id 寫回；更新時先保存舊版本，復原時寫回。
- `DeleteCommand`：刪除前保存整筆資料，復原時以原 id 寫回。
- `CompositeCommand`：例如匯入 CSV（建立缺少的錢包、分類，再新增每一筆交易）、定期交易一次補入帳多筆、
  合併分類（移動交易再刪除分類），整批只佔復原歷史中的一步。
- `CommandManager`（Invoker）：在資料庫交易中執行指令，失敗時資料完全不變；保存最多 100 步的復原歷史。

UI 的選單項目與快捷鍵使用 Swing 的 `Action`，同一個動作物件同時放在選單、按鈕與按鍵綁定中，
復原、重做的名稱與啟用狀態依 `HistoryChangedEvent` 更新。

## Observer

```java
EventBus bus = app.bus();
bus.subscribe(DataChangedEvent.class, event -> {
    if (event.affects(Topic.TRANSACTIONS)) { ... }
});
```

`CommandManager` 在每次執行、復原、重做後發布 `DataChangedEvent`。`AppShell` 收到後，
目前顯示的頁面立刻重新整理，其他關心這類資料的頁面標記為過期，切換過去時才重新整理；
側邊欄的本月小樹與狀態列的訊息也是事件的訂閱者。發布事件的 service 不知道有哪些畫面存在。

## Chain of Responsibility

查詢助手先把問題解析成 `Question`（正規化文字、時間範圍、提到的分類、錢包或目標），
再依序詢問每個 `QueryHandler` 能不能回答，由第一個能回答的負責：

```java
handlers = List.of(
        QueryHandler.of(q -> q.isBlank() || q.has("可以問", "幫助"), q -> helpText()),
        QueryHandler.of(q -> q.goal() != null, a::goals),
        QueryHandler.of(q -> q.has("比較", "比上", "增加", "減少"), a::compare),
        QueryHandler.of(q -> q.has("預算", "超支"), a::budget),
        ...
        QueryHandler.of(q -> q.has("最近", "最新"), q -> a.recent()));
```

新增一種問題只要在清單中加一個 handler。時間範圍由 `PeriodParser` 處理（今天、本週、上個月、3 月、
2026 年 5 月、最近 30 天、今年……，也能讀中文數字）。

## Builder 與 Value Object

```java
Transaction lunch = Transaction.builder()
        .expense()
        .date(LocalDate.of(2026, 10, 28))
        .time(LocalTime.of(12, 30))
        .amount(Money.parse("120"))
        .categoryId(food.id())
        .walletId(cash.id())
        .note("牛肉麵")
        .build();          // 建構子檢查所有規則，不合法時丟出 ValidationException
```

`Money` 是不可變的值物件：以值比較（`Money.of(12)` 等於 `Money.parse("12.00")`），所有運算回傳新物件，
格式化與解析（千分位、全形數字、`NT$`）都集中在這個類別。`DateRange` 提供 `ofMonth`、`previous()` 等方法，
避免日期計算散落在各處。

## 依賴注入與組合根

`AppContext` 是唯一用 `new` 建立 service 的地方：

```java
this.commands = new CommandManager(storage, bus);
this.wallets = new WalletService(storage, commands);
this.transactions = new TransactionService(storage, commands, clock);
this.statistics = new StatisticsService(storage, wallets, clock);
```

其他類別只透過建構子取得需要的物件，因此測試可以注入記憶體資料庫與固定的 `Clock`。
