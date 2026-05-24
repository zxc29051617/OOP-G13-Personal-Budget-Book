# Personal Budget Book 個人記帳本

第 13 組期末專題：一個以 Java Swing 製作的桌面版個人記帳 App。
專案提供快速記帳、錢包管理、收支分析、每月預算與種樹激勵機制，並使用 MySQL 儲存資料。

GitHub Repository:
https://github.com/zxc29051617/OOP-G13-Personal-Budget-Book

## 專案功能

- 快速新增收入 / 支出紀錄
- 支援日期與時間欄位
- 管理多個錢包，例如現金、銀行帳戶、電子支付
- 顯示總餘額、本月收入、本月支出與預算使用率
- 依分類統計本月支出
- 查看全部交易紀錄
- 設定每月支出預算
- 種樹激勵機制：
  - 新增一筆記帳 = 澆水一次
  - 本月澆水 20 次 = 完成一棵樹
  - 本月 0 次記帳 = 小樹枯萎
  - 1 到 19 次 = 成長中

## 介面設計

本專案參考手機 Finance App 的操作方式，並將其實作為 Java Swing 桌面應用程式。

主要介面包含：

| 頁面 | 說明 |
|---|---|
| 首頁 | 顯示總餘額、本月收入、本月支出、預算使用率、種樹進度與最近交易 |
| 快速記帳 | 輸入類型、日期、時間、分類、錢包、金額與備註 |
| 財務分析 | 顯示本月收入、支出、結餘與支出分類圖表 |
| 錢包管理 | 管理現金、銀行帳戶、電子支付等錢包 |
| 交易紀錄 | 查看與刪除所有交易 |
| 設定 | 設定每月預算並查看種樹進度 |

介面特色：

- App 風格底部導覽列
- 右上角設定入口
- 圓角卡片式資訊區塊
- 淺色系背景與綠色主視覺
- 使用 FlatLaf 改善 Swing 預設外觀

## 使用技術

- Java 25
- Java Swing
- MySQL 8.0
- JDBC
- MySQL Connector/J
- FlatLaf

## 專案結構

```text
PersonalBudgetBook/
├─ src/
│  ├─ app/            程式進入點
│  ├─ model/          資料模型
│  ├─ persistence/    MySQL 資料存取層
│  ├─ service/        統計與商業邏輯
│  └─ ui/             Swing 使用者介面
├─ db/
│  └─ schema.sql      MySQL 資料表設計
├─ lib/
│  └─ README.md       本機 jar 放置說明
├─ data/              本機設定與資料，不上傳 GitHub
├─ out/               編譯輸出，不上傳 GitHub
├─ MYSQL_SETUP.md     MySQL 設定說明
├─ run.bat            Windows 執行腳本
└─ README.md
```

## 程式架構

本專案採用簡單分層架構：

```text
UI Panel
  ↓
BudgetBookFrame
  ↓
StatsService / BudgetBookStore
  ↓
MySQL Database
```

各層說明：

| 層級 | 主要檔案 | 說明 |
|---|---|---|
| app | `BudgetBookApp.java` | 啟動程式、設定 Look and Feel、載入資料 |
| model | `Account.java`, `Transaction.java`, `BudgetSettings.java` | 定義帳戶、交易與設定資料 |
| persistence | `BudgetBookStore.java` | 負責連線 MySQL、建立資料表、讀寫資料 |
| service | `StatsService.java` | 計算總餘額、月收入、月支出、分類支出與種樹進度 |
| ui | `HomePanel.java`, `QuickEntryPanel.java`, `AnalysisPanel.java` 等 | Swing 畫面 |

## 資料庫設計

本專案使用 MySQL，資料庫名稱預設為：

```text
personal_budget_book
```

主要資料表：

### accounts

儲存錢包資料。

| 欄位 | 說明 |
|---|---|
| id | 錢包 ID |
| name | 錢包名稱 |
| type | 錢包類型 |
| initial_balance | 初始金額 |

### transactions

儲存收入與支出紀錄。

| 欄位 | 說明 |
|---|---|
| id | 交易 ID |
| date | 交易日期 |
| transaction_time | 交易時間 |
| kind | `INCOME` 或 `EXPENSE` |
| category | 分類 |
| account_id | 對應錢包 ID |
| amount | 金額 |
| note | 備註 |

### settings

儲存系統設定。

| 欄位 | 說明 |
|---|---|
| id | 設定 ID |
| monthly_limit | 每月支出預算 |
| points | 保留欄位 |

關聯：

```text
accounts 1 ---- N transactions
```

## 執行環境

建議環境：

```text
Windows 10 / 11
Java 25
MySQL Server 8.0
PowerShell
```

本專案需要下列 jar 放在 `lib/`：

```text
lib/mysql-connector-j-*.jar
lib/flatlaf-*.jar
```

注意：`lib/*.jar` 不會上傳到 GitHub，因此下載專案後需要自行放入。

## MySQL 設定

請先確認 MySQL 服務已啟動。

Windows 可用以下指令檢查：

```powershell
Get-Service | Where-Object { $_.Name -like "*mysql*" }
```

預設連線：

```properties
url=jdbc:mysql://localhost:3306/personal_budget_book?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8
user=root
password=
```

如果 MySQL root 有密碼，請建立：

```text
data/database.properties
```

範例：

```properties
url=jdbc:mysql://localhost:3306/personal_budget_book?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8
user=root
password=你的MySQL密碼
```

程式啟動時會自動建立需要的資料表。
也可以參考 `db/schema.sql` 手動建立。

## 執行方式

在 PowerShell 進入專案資料夾：

```powershell
cd C:\Users\user\Desktop\PersonalBudgetBook
.\run.bat
```

`run.bat` 會執行：

```bat
javac -encoding UTF-8 -cp "lib/*" -d out src\app\*.java src\model\*.java src\persistence\*.java src\service\*.java src\ui\*.java
java -cp "out;lib/*" app.BudgetBookApp
```

## 常見問題

### 1. 找不到 MySQL driver

請確認 `lib/` 內有：

```text
mysql-connector-j-*.jar
```

### 2. 連不上 MySQL

請確認：

- MySQL 服務正在執行
- `data/database.properties` 的帳號密碼正確
- 資料庫連線網址是 `localhost:3306`

### 3. 出現 FlatLaf native access warning

若使用 Java 25，執行時可能出現類似警告：

```text
WARNING: A restricted method in java.lang.System has been called
```

這是 FlatLaf 在新版 Java 下的相容性提醒，不影響目前程式執行。

## GitHub 繳交提醒

繳交前請確認：

- GitHub Repository 已設為 Public
- PPT 或 Report 內有附 GitHub 連結
- 使用無痕視窗測試連結能正常打開
- 不要上傳 `data/database.properties`，避免密碼外洩
- 不要上傳 `out/` 與大型 jar 檔
