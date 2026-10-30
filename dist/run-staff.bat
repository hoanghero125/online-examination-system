@echo off
rem Ung dung quan ly (quan tri vien, giang vien) - cung la may cham bai
rem Can Java 17 tro len (java -version). Chay tu thu muc chua tep jar.
cd /d "%~dp0"
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -jar oes-staff.jar %*
if errorlevel 1 pause
