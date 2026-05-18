# MySQL 設定說明

這個專案現在已經改成用 MySQL 儲存資料，不再把主要資料寫回 CSV。

## 1. 安裝並啟動 MySQL

先安裝 MySQL Server，並確認它有在本機 `localhost:3306` 執行。

程式預設會連到：

```properties
url=jdbc:mysql://localhost:3306/personal_budget_book?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8
user=root
password=
```

如果你的 root 有密碼，請看第 3 步設定。

## 2. 加入 Java MySQL driver

下載 MySQL Connector/J，然後把 jar 檔放到：

```text
lib/mysql-connector-j-*.jar
```

`lib/*.jar` 已經被 `.gitignore` 忽略，所以每台電腦都要自己放一次 driver。

## 3. 設定帳號密碼

如果你的 MySQL 帳號或密碼不同，建立這個檔案：

```text
data/database.properties
```

範例：

```properties
url=jdbc:mysql://localhost:3306/personal_budget_book?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8
user=root
password=你的密碼
```

也可以改用環境變數：

```text
PBB_DB_URL
PBB_DB_USER
PBB_DB_PASSWORD
```

## 4. 執行

```bat
run.bat
```

第一次啟動時，程式會自動建立 `accounts`、`transactions`、`settings` 三張資料表。如果 MySQL 裡還沒有帳戶資料，但 `data/` 裡有舊的 CSV，程式會嘗試匯入一次。
