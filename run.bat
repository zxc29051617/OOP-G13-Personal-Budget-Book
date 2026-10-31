@echo off
chcp 65001 >nul
rem 個人記帳本啟動腳本（Windows）：只需要 Java 21 以上。
rem 第一次執行會用 Maven Wrapper 建置 target\personal-budget-book.jar。
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo 找不到 java，請先安裝 Java 21 以上（例如 Eclipse Temurin 21）。
    pause
    exit /b 1
)

if not exist target\personal-budget-book.jar (
    echo 第一次執行，正在建置執行檔（需要網路下載相依套件）...
    call mvnw.cmd -q -B -DskipTests package
    if errorlevel 1 (
        pause
        exit /b 1
    )
)
java -jar target\personal-budget-book.jar %*
if errorlevel 1 pause
