# 架構

個人記帳本分成五層，上層只依賴下層的介面：

```mermaid
flowchart TB
    ui["ui<br/>Swing 頁面、表單、元件"]
    service["service<br/>商業邏輯、統計、報表、CSV、查詢助手"]
    command["command / event<br/>可復原的指令、事件匯流排"]
    repository["repository<br/>Repository 介面、Storage、StorageFactory"]
    jdbc["repository.jdbc<br/>JDBC 實作、SqlDialect、SchemaMigrator"]
    model["model<br/>不可變的實體與值物件"]
    db[("SQLite（預設）<br/>或 MySQL")]

    ui --> service
    service --> command
    service --> repository
    command --> repository
    repository --> jdbc
    jdbc --> db
    ui -.-> model
    service -.-> model
    repository -.-> model
```

| 套件 | 職責 | 主要類別 |
|---|---|---|
| `budgetbook.app` | 程式進入點、解析命令列參數與設定檔、headless smoke test | `BudgetBookApp`、`AppConfig`、`SmokeTest` |
| `budgetbook.model` | 不可變的實體（record）與值物件，建構時檢查資料 | `Money`、`Transaction`、`Wallet`、`Category`、`Budget`、`RecurringRule`、`SavingsGoal`、`DateRange` |
| `budgetbook.exception` | 自訂例外，訊息是給使用者看的中文 | `ValidationException`、`EntityInUseException`、`StorageException` |
| `budgetbook.repository` | 資料存取介面與資料庫設定 | `Repository<T>`、`TransactionRepository`、`Storage`、`StorageFactory` |
| `budgetbook.repository.jdbc` | 兩種資料庫共用的 JDBC 實作 | `JdbcRepository<T>`、`JdbcStorage`、`SqlDialect`、`SchemaMigrator` |
| `budgetbook.event` | 資料變更通知 | `EventBus`、`DataChangedEvent`、`HistoryChangedEvent` |
| `budgetbook.command` | 復原與重做 | `Command`、`SaveCommand`、`DeleteCommand`、`CompositeCommand`、`CommandManager` |
| `budgetbook.service` | 商業規則與計算，組合根 | `TransactionService`、`BudgetService`、`RecurringService`、`TreeService`、`AppContext` |
| `budgetbook.service.report` | 月報、年報與匯出 | `ReportService`、`ReportPeriod`、`ReportExporter` |
| `budgetbook.service.io` | CSV 匯入匯出 | `Csv`、`TransactionCsvService` |
| `budgetbook.service.assistant` | 規則式查詢助手 | `QueryAssistantService`、`QueryHandler`、`PeriodParser` |
| `budgetbook.ui` | Swing 介面，只呼叫 service | `AppShell`、`Page`、`UiContext`、`MainFrame` |

## 領域模型

所有實體都是 Java `record`：欄位不能修改，建構子檢查資料是否合法（金額必須大於 0、轉帳的兩個錢包不能相同……），
修改時建立新物件（`withName`、`toBuilder()`）。金額一律使用 `Money`，內部是兩位小數的 `BigDecimal`。

```mermaid
classDiagram
    class Entity {
        <<interface>>
        +id() long
        +isNew() boolean
    }
    class Money {
        <<ValueObject>>
        -BigDecimal value
        +parse(String) Money$
        +plus(Money) Money
        +minus(Money) Money
        +ratioOf(Money) double
        +format() String
    }
    class Transaction {
        <<record>>
        +TransactionType type
        +LocalDate date
        +LocalTime time
        +Money amount
        +Long categoryId
        +long walletId
        +Long toWalletId
        +String note
        +balanceEffectOn(long) Money
        +builder() Builder$
    }
    class Wallet {
        <<record>>
        +String name
        +WalletType type
        +Money initialBalance
        +boolean archived
    }
    class Category {
        <<record>>
        +String name
        +CategoryKind kind
        +int color
        +String icon
    }
    class Budget {
        <<record>>
        +Long categoryId
        +Money monthlyLimit
        +isOverall() boolean
    }
    class RecurringRule {
        <<record>>
        +Frequency frequency
        +int interval
        +LocalDate startDate
        +int postedCount
        +dueDates(LocalDate) List
        +toTransaction(LocalDate) Transaction
    }
    class SavingsGoal {
        <<record>>
        +Money target
        +Money saved
        +deposit(Money) SavingsGoal
        +monthlyNeeded(LocalDate) Optional
    }
    class BudgetStatus {
        <<record>>
        +Money spent
        +ratio() double
        +level() BudgetLevel
    }
    class MonthlyTree {
        <<record>>
        +YearMonth month
        +int records
        +int goal
        +stage() TreeStage
    }
    class TransactionType {
        <<enumeration>>
        EXPENSE
        INCOME
        TRANSFER
    }
    class TreeStage {
        <<enumeration>>
        WITHERED
        SEED
        SPROUT
        SAPLING
        YOUNG_TREE
        FULL_TREE
    }

    Entity <|.. Transaction
    Entity <|.. Wallet
    Entity <|.. Category
    Entity <|.. Budget
    Entity <|.. RecurringRule
    Entity <|.. SavingsGoal
    Transaction --> TransactionType
    Transaction --> Money
    Transaction ..> Wallet : walletId / toWalletId
    Transaction ..> Category : categoryId
    Budget ..> Category : categoryId（null 為總預算）
    RecurringRule ..> Transaction : 產生
    BudgetStatus --> Budget
    MonthlyTree --> TreeStage
```

## 資料存取

service 只認得 `Storage` 與各個 `Repository` 介面。`StorageFactory` 依設定建立 SQLite 或 MySQL 的 `JdbcStorage`；
兩者共用同一份 JDBC 程式，差異（如何連線、建表 SQL）由 `SqlDialect` 處理。

```mermaid
classDiagram
    class Repository~T~ {
        <<interface>>
        +findById(long) Optional~T~
        +findAll() List~T~
        +save(T) T
        +restore(T) T
        +deleteById(long)
    }
    class TransactionRepository {
        <<interface>>
        +find(TransactionQuery) List
        +findBetween(DateRange) List
        +netChangeByWallet(LocalDate) Map
    }
    class Storage {
        <<interface>>
        +wallets() WalletRepository
        +transactions() TransactionRepository
        +budgets() BudgetRepository
        +inTransaction(Supplier) T
        +info() StorageInfo
    }
    class StorageFactory {
        +open(StorageConfig) Storage$
    }
    class JdbcRepository~T~ {
        <<abstract>>
        #map(ResultSet) T
        #values(T) Object[]
        #withId(T, long) T
    }
    class JdbcTransactionRepository
    class JdbcStorage
    class JdbcSession {
        +query(String, RowMapper, params) List
        +update(String, params) int
        +inTransaction(Supplier) T
    }
    class SqlDialect {
        <<interface>>
        +openConnection() Connection
        +migrationFolder() String
    }
    class SqliteDialect
    class MySqlDialect
    class SchemaMigrator {
        +migrate() int
    }

    Repository <|-- TransactionRepository
    Repository <|.. JdbcRepository
    JdbcRepository <|-- JdbcTransactionRepository
    TransactionRepository <|.. JdbcTransactionRepository
    Storage <|.. JdbcStorage
    JdbcStorage o-- JdbcTransactionRepository
    JdbcStorage --> JdbcSession
    JdbcSession --> SqlDialect
    SqlDialect <|.. SqliteDialect
    SqlDialect <|.. MySqlDialect
    StorageFactory ..> JdbcStorage : 建立
    StorageFactory ..> SchemaMigrator : 升級 schema
```

### 資料表

金額以「分」為單位的整數保存（避免浮點誤差），日期與時間以 ISO 格式保存（SQLite 為文字、MySQL 為 `DATE`／`TIME`）。

```mermaid
erDiagram
    wallets ||--o{ transactions : "wallet_id / to_wallet_id"
    categories ||--o{ transactions : category_id
    categories ||--o| budgets : category_id
    wallets ||--o{ recurring_rules : wallet_id
    categories ||--o{ recurring_rules : category_id

    wallets {
        bigint id PK
        string name UK
        string type
        bigint initial_cents
        int color
        boolean archived
    }
    categories {
        bigint id PK
        string name
        string kind
        int color
        string icon
    }
    transactions {
        bigint id PK
        string type
        date tx_date
        time tx_time
        bigint amount_cents
        bigint category_id FK
        bigint wallet_id FK
        bigint to_wallet_id FK
        string note
        bigint recurring_rule_id
    }
    budgets {
        bigint id PK
        bigint category_id FK
        bigint limit_cents
    }
    recurring_rules {
        bigint id PK
        string name
        string type
        bigint amount_cents
        string frequency
        int interval_count
        date start_date
        date end_date
        int posted_count
        boolean active
    }
    savings_goals {
        bigint id PK
        string name
        bigint target_cents
        bigint saved_cents
        date deadline
    }
    settings {
        string setting_key PK
        string setting_value
    }
```

### Schema 版本管理

`schema_version` 資料表記錄目前的版本。啟動時 `SchemaMigrator` 依序執行尚未套用的
`src/main/resources/db/<sqlite|mysql>/V<版本>__<名稱>.sql`，執行完再把版本寫入 `schema_version`
（SQLite 的整個版本在同一個資料庫交易中完成；MySQL 的建表指令會自動提交）。

| 版本 | 內容 |
|---|---|
| V1 | 錢包、分類、交易、設定 |
| V2 | 預算、定期交易、儲蓄目標，並在交易表加入 `recurring_rule_id` |

新增欄位或資料表時，在 `SchemaMigrator.MIGRATIONS` 加一個版本並撰寫兩種資料庫的 SQL；舊的資料庫會在下次啟動時自動升級。

## 修改資料的流程

所有修改都經過 `CommandManager`：指令在一個資料庫交易中執行，成功後放進復原歷史，再發布事件讓畫面更新。
以新增一筆交易為例：

```mermaid
sequenceDiagram
    actor User as 使用者
    participant Form as TransactionForm
    participant Service as TransactionService
    participant Manager as CommandManager
    participant Command as SaveCommand
    participant Repo as TransactionRepository
    participant Bus as EventBus
    participant Shell as AppShell

    User->>Form: 輸入金額、分類，按「儲存」
    Form->>Service: add(transaction)
    Service->>Service: 檢查錢包、分類是否存在且類型相符
    Service->>Manager: execute(new SaveCommand(...))
    Manager->>Command: execute()（在資料庫交易中）
    Command->>Repo: save(transaction)
    Repo-->>Command: 帶有新 id 的交易
    Manager->>Bus: DataChangedEvent(TRANSACTIONS)
    Manager->>Bus: CommandEvent、HistoryChangedEvent
    Bus->>Shell: 通知
    Shell->>Shell: 目前頁面重新整理，其他頁面標記為過期
```

按 Ctrl+Z 時 `CommandManager.undo()` 取出最近的指令並呼叫 `undo()`：新增的資料被刪除、修改的資料寫回舊版本、
刪除的資料以原本的 id 寫回。匯入 CSV、定期交易入帳這類一次修改多筆的操作，會合併成一個 `CompositeCommand`，一次就能復原。

## 服務與組合根

`AppContext` 是組合根：建立 `Storage`、`EventBus`、`CommandManager` 與所有 service，並以建構子注入彼此需要的物件。
需要「今天」的 service 都注入 `java.time.Clock`，測試時使用固定日期。

```mermaid
classDiagram
    class AppContext {
        +open(StorageConfig, Clock) AppContext$
        +transactions() TransactionService
        +budgets() BudgetService
        +recurring() RecurringService
        +reports() ReportService
        +assistant() QueryAssistantService
    }
    class CommandManager {
        +execute(Command) Command
        +executeBatch(String, Function) R
        +undo()
        +redo()
    }
    class Command {
        <<interface>>
        +execute()
        +undo()
        +redo()
        +description() String
    }
    class SaveCommand~T~
    class DeleteCommand~T~
    class CompositeCommand
    class EventBus {
        +subscribe(Class, Consumer) Subscription
        +publish(Object)
    }
    class TransactionService
    class BudgetService
    class RecurringService
    class TreeService
    class ReportService
    class ReportPeriod {
        <<interface>>
        +range() DateRange
        +previous() ReportPeriod
        +series(StatisticsService) List
    }
    class MonthlyPeriod
    class YearlyPeriod
    class ReportExporter {
        <<interface>>
        +write(Report, Writer)
    }
    class CsvReportExporter
    class HtmlReportExporter
    class QueryHandler {
        <<interface>>
        +canAnswer(Question) boolean
        +answer(Question) String
    }

    AppContext *-- CommandManager
    AppContext *-- EventBus
    AppContext *-- TransactionService
    AppContext *-- BudgetService
    AppContext *-- RecurringService
    AppContext *-- TreeService
    AppContext *-- ReportService
    CommandManager o-- Command
    CommandManager --> EventBus
    Command <|.. SaveCommand
    Command <|.. DeleteCommand
    Command <|.. CompositeCommand
    CompositeCommand o-- Command
    TransactionService --> CommandManager
    RecurringService --> CommandManager
    ReportService ..> ReportPeriod
    ReportPeriod <|.. MonthlyPeriod
    ReportPeriod <|.. YearlyPeriod
    ReportExporter <|.. CsvReportExporter
    ReportExporter <|.. HtmlReportExporter
```

## 使用者介面

`MainFrame` 只負責視窗與選單，內容是 `AppShell`：左邊導覽列、中間以 `CardLayout` 切換的頁面、下方狀態列。
每個頁面繼承 `Page`，宣告自己關心的資料（`topics()`）並在 `reload()` 中向 service 取得資料；
頁面不直接存取資料庫，也不包含商業規則。`AppShell` 不依賴 `JFrame`，因此測試可以在沒有螢幕的環境中建立整個介面並畫成圖片。

```mermaid
classDiagram
    class Page {
        <<abstract>>
        +id() String
        +topics() Set~Topic~
        #reload()
        +refresh()
    }
    class AppShell {
        +show(String)
        +setDarkTheme(boolean)
    }
    class UiContext {
        +app() AppContext
        +open(Form) boolean
        +run(Runnable) boolean
    }
    class FormDialog {
        +show(UiContext, Form) boolean$
    }
    class Form {
        <<interface>>
        +content() JComponent
        +save()
    }

    Page <|-- DashboardPage
    Page <|-- TransactionsPage
    Page <|-- BudgetsPage
    Page <|-- ReportsPage
    Page <|-- ForestPage
    Page <|-- AssistantPage
    AppShell o-- Page
    Page --> UiContext
    UiContext ..> FormDialog
    FormDialog --> Form
    Form <|.. TransactionForm
    Form <|.. WalletForm
    Form <|.. RecurringForm
```

切換深色模式時，`Theme` 重新套用 FlatLaf；顏色由 `Palette` 依目前主題提供，圖表在頁面重新整理時以新顏色重建。
