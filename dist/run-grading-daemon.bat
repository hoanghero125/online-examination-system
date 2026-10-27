@echo off
rem May cham khong giao dien, chay tren may chu CSDL. Dung bang Ctrl+C
rem Can Java 17 tro len (java -version). Chay tu thu muc chua tep jar.
cd /d "%~dp0"
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -cp oes-staff.jar com.group2.oes.app.GradingDaemon db-staff.properties %*
if errorlevel 1 pause
