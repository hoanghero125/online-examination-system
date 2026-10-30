@echo off
rem Mo ung dung thi (ExamClientApp). Chua co db-client.properties thi ung dung hoi dia chi may chu.
setlocal
chcp 65001 >nul
cd /d "%~dp0.."
call "%~dp0lib\check-jdk.bat" || goto :fail
call mvnw.cmd -q javafx:run@client || goto :fail
exit /b 0

:fail
pause
exit /b 1
