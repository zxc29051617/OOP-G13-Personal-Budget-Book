# Third-Party Notices

個人記帳本的原始碼由第 13 組撰寫。執行檔（`personal-budget-book.jar`）會一併打包下列第三方函式庫，
各函式庫依其授權條款散布，版權屬於原作者。

| 函式庫 | 用途 | 授權 | 專案網址 |
|---|---|---|---|
| FlatLaf | Swing 外觀（淺色／深色主題） | Apache License 2.0 | https://github.com/JFormDesigner/FlatLaf |
| XChart | 圓餅圖、長條圖、折線圖 | Apache License 2.0 | https://github.com/knowm/XChart |
| SQLite JDBC (xerial) | 內建 SQLite 資料庫 | Apache License 2.0 | https://github.com/xerial/sqlite-jdbc |
| MySQL Connector/J | 選用的 MySQL 後端 | GPL 2.0 with Universal FOSS Exception | https://github.com/mysql/mysql-connector-j |

建置與測試時另外使用（不會打包進執行檔）：

| 工具 | 用途 | 授權 |
|---|---|---|
| JUnit 5 | 單元測試 | Eclipse Public License 2.0 |
| JaCoCo | 測試覆蓋率報告 | Eclipse Public License 2.0 |
| Apache Maven Wrapper（`mvnw`、`mvnw.cmd`） | 只有 Java 時也能建置 | Apache License 2.0 |

本專案沒有複製上述專案的原始碼；介面與功能構想參考了 GnuCash、HomeBank、Money Manager Ex、
Firefly III、Actual Budget 與記帳城市等記帳軟體，程式碼皆為本專案自行撰寫。

授權全文：

- Apache License 2.0：https://www.apache.org/licenses/LICENSE-2.0
- Eclipse Public License 2.0：https://www.eclipse.org/legal/epl-2.0/
- GPL 2.0 with Universal FOSS Exception：https://oss.oracle.com/licenses/universal-foss-exception/
