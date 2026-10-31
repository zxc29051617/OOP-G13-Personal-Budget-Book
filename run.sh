#!/bin/sh
# 個人記帳本啟動腳本（macOS / Linux）：只需要 Java 21 以上。
# 第一次執行會用 Maven Wrapper 建置 target/personal-budget-book.jar。
set -e
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
    echo "找不到 java，請先安裝 Java 21 以上（例如 Eclipse Temurin 21）。"
    exit 1
fi
version=$(java -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.specification.version = //p')
case "$version" in
    1.*|[0-9]|1[0-9]|20)
        echo "需要 Java 21 以上，目前是 Java $version。"
        exit 1
        ;;
esac

JAR=target/personal-budget-book.jar
if [ ! -f "$JAR" ]; then
    echo "第一次執行，正在建置執行檔（需要網路下載相依套件）..."
    ./mvnw -q -B -DskipTests package
fi
exec java -jar "$JAR" "$@"
