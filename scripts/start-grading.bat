@echo off
rem May cham khong giao dien, dung khi khong mo StaffApp trong gio thi. Dong cua so de dung.
setlocal
chcp 65001 >nul
cd /d "%~dp0.."
call "%~dp0lib\check-jdk.bat" || goto :fail
rem Tieng Viet do chuong trinh Java in ra hien dung dau trong cua so dong lenh.
set "MAVEN_OPTS=-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 %MAVEN_OPTS%"
if not exist db-staff.properties (echo LOI: Chua co db-staff.properties: chay scripts\setup.bat truoc. & goto :fail)
call mvnw.cmd -q compile || goto :fail
call mvnw.cmd -q exec:java@grading "-Dexec.args=db-staff.properties" || goto :fail
exit /b 0

:fail
pause
exit /b 1
