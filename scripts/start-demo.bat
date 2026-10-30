@echo off
rem Mo ca ung dung quan ly va ung dung thi tren cung may, de thu hoac demo mot minh.
rem Moi ung dung chay trong mot cua so dong lenh rieng; dong cua so do thi ung dung tat.
setlocal
chcp 65001 >nul
cd /d "%~dp0.."
call "%~dp0lib\check-jdk.bat" || goto :fail
rem Build mot lan truoc, de hai lenh chay song song khong cung bien dich vao target\.
call mvnw.cmd -q compile || goto :fail
start "StaffApp - ung dung quan ly" cmd /c mvnw.cmd -q javafx:run@staff
ping -n 4 127.0.0.1 >nul
start "ExamClientApp - ung dung thi" cmd /c mvnw.cmd -q javafx:run@client
exit /b 0

:fail
pause
exit /b 1
