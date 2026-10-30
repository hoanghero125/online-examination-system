@echo off
rem Kiem tra may co JDK 17 tro len (can javac; chi co JRE thi Maven khong bien dich duoc).
rem Goi tu script khac: call "%~dp0lib\check-jdk.bat" || goto :fail
set "OES_JAVAC=javac"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "OES_JAVAC=%JAVA_HOME%\bin\javac.exe"
call "%OES_JAVAC%" -version >nul 2>&1
if errorlevel 1 (
    echo LOI: Chua co JDK ^(khong tim thay javac^). Cai JDK 17 tro len, vi du Eclipse Temurin 17: https://adoptium.net
    exit /b 1
)
set "OES_JDK_VERSION="
for /f "tokens=2" %%v in ('call "%OES_JAVAC%" -version 2^>^&1') do set "OES_JDK_VERSION=%%v"
set "OES_JDK_MAJOR=0"
for /f "delims=." %%m in ("%OES_JDK_VERSION%") do set "OES_JDK_MAJOR=%%m"
if %OES_JDK_MAJOR% LSS 17 (
    echo LOI: Can JDK 17 tro len, may dang co JDK %OES_JDK_VERSION%.
    exit /b 1
)
exit /b 0
