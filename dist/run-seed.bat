@echo off
rem Tao du lieu mau cho buoi demo (chay mot lan tren CSDL moi)
rem Can Java 17 tro len (java -version). Chay tu thu muc chua tep jar.
cd /d "%~dp0"
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -cp oes-staff.jar com.group2.oes.tools.SeedTool db-staff.properties %*
if errorlevel 1 pause
