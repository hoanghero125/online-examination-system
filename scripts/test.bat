@echo off
rem Chay kiem thu.
rem   scripts\test.bat          kiem thu khong can MySQL (DAO trong bo nho)
rem   scripts\test.bat --db     them kiem thu voi MySQL that: dung CSDL rieng oes_test (xoa va tao lai moi lan),
rem                             ghi test-staff.properties, test-client.properties roi chay toan bo.
rem Tuy chon khac sau --db chuyen cho DatabaseSetup, dat trong ngoac kep neu co dau "=".
rem Mat khau trong db-*.properties khong duoc chua ky tu %% hoac ! (cmd hieu nham).
setlocal
chcp 65001 >nul
cd /d "%~dp0.."
call "%~dp0lib\check-jdk.bat" || goto :fail

if /i not "%~1"=="--db" (
    call mvnw.cmd test || goto :fail
    exit /b 0
)
set "REST=%*"
set "REST=%REST:*--db=%"
if defined REST set "REST=%REST:"=%"
rem Tieng Viet do chuong trinh Java in ra hien dung dau trong cua so dong lenh.
set "MAVEN_OPTS=-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 %MAVEN_OPTS%"

rem oes_staff, oes_client dung chung cho moi CSDL tren may: giu dung mat khau da ghi trong cau hinh hien co,
rem de dung CSDL kiem thu khong lam hong db-staff.properties, db-client.properties.
set "EXTRA="
if exist db-staff.properties for /f "usebackq tokens=1* delims==" %%a in ("db-staff.properties") do if "%%a"=="db.password" set "EXTRA=%EXTRA% --staff-password=%%b"
if exist db-client.properties for /f "usebackq tokens=1* delims==" %%a in ("db-client.properties") do if "%%a"=="db.password" set "EXTRA=%EXTRA% --client-password=%%b"

call mvnw.cmd -q compile || goto :fail
call mvnw.cmd -q exec:java@setup "-Dexec.args=--database=oes_test --reset --no-seed --staff-config=test-staff.properties --client-config=test-client.properties%EXTRA% %REST%" || goto :fail
set "OES_TEST_DB=test-staff.properties"
set "OES_TEST_DB_CLIENT=test-client.properties"
call mvnw.cmd test || goto :fail
exit /b 0

:fail
pause
exit /b 1
