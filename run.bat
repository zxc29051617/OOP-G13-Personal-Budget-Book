@echo off
cd /d "%~dp0"
javac -encoding UTF-8 -d out src\app\*.java src\model\*.java src\persistence\*.java src\service\*.java src\ui\*.java
if errorlevel 1 pause & exit /b 1
java -cp out app.BudgetBookApp
