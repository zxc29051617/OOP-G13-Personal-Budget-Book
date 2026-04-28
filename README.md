# Personal Budget Book

第 13 組自選題：個人記帳管理系統。

## 專案目標

本系統用來整合現金、銀行、信用卡、電子支付等多種消費管道，降低個人財務管理分散的問題。系統提供收支紀錄、帳戶管理、即時統計、月預算提醒、歷史查詢、資料視覺化，以及以點數和種樹等級呈現的遊戲化激勵。

## 四層式架構

```text
src/
├─ app/           程式入口
├─ model/         資料模型：帳戶、交易、預算設定
├─ persistence/   資料儲存層：CSV 讀寫與資料管理
├─ service/       商業邏輯層：統計、結餘、分類分析
└─ ui/            使用者介面層：Swing 視窗與各功能頁面
```

## 功能

- 收支紀錄：支援收入、支出、分類、日期、帳戶、金額、備註。
- 帳戶管理：可建立現金、銀行、信用卡、電子支付等多帳戶。
- 即時統計：每次記帳後自動更新本月收入、支出、結餘與預算使用率。
- 互動式 GUI：使用 Java Swing 製作首頁、快速記帳、分析、錢包、歷史、設定頁。
- 集點獎勵：每新增一筆記帳紀錄可獲得 10 點，點數會提升種樹等級。
- 月開銷限額：可設定每月支出上限。
- 歷史紀錄查詢：可查看並刪除所有交易紀錄。
- 資料視覺化：分析頁會依分類顯示本月支出長條圖。

## 執行方式

可直接雙擊 `run.bat`，或使用 PowerShell：

```powershell
cd C:\Users\user\Desktop\OOP_Java\PersonalBudgetBook
.\run.bat
```

手動編譯執行：

```powershell
javac -encoding UTF-8 -d out src\app\*.java src\model\*.java src\persistence\*.java src\service\*.java src\ui\*.java
java -cp out app.BudgetBookApp
```

資料會儲存在 `data` 資料夾中的 CSV 檔案。

## GitHub 協作方式

本專案採用 `main / dev / feature` 分支協作流程。

### 分支說明

- `main`：穩定版本，最後繳交與 Demo 使用。
- `dev`：開發整合版本，組員完成的功能會先合併到這裡。
- `feature/功能名稱`：個人功能分支，每位組員開自己的分支開發。

### 第一次下載專案

```bash
git clone https://github.com/zxc29051617/OOP-G13-Personal-Budget-Book.git
cd OOP-G13-Personal-Budget-Book
git checkout dev
git pull origin dev
```


### 開始開發新功能
請先從 dev 分支建立自己的功能分支：
```bash
git checkout dev
git pull origin dev
git checkout -b feature/功能名稱
```

修改完成後上傳：
```bash
git add .
git commit -m "說明本次修改內容"
git push origin feature/功能名稱
```
