@echo off
rem Mo ung dung quan ly (StaffApp). Dang mo la may cham bai dang chay.
setlocal
chcp 65001 >nul
cd /d "%~dp0.."
call "%~dp0lib\check-jdk.bat" || goto :fail
if not exist db-staff.properties echo Chua co db-staff.properties: chay scripts\setup.bat truoc (hoac nhap may chu o man hinh dau).
call mvnw.cmd -q javafx:run@staff || goto :fail
exit /b 0

:fail
pause
exit /b 1
