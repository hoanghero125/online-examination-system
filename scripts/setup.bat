@echo off
rem Dung moi thu de chay du an tren may nay, mot lan:
rem   kiem tra JDK -> build -> tao CSDL oes (schema.sql, security.sql) -> ghi db-staff.properties,
rem   db-client.properties -> tao du lieu mau (40 sinh vien lop D24CQCE07-B, 60 cau hoi, 1 ca thi mo 2 gio).
rem Can MySQL 8 dang chay tren may; chua co MySQL thi dung scripts\setup-docker.bat.
rem
rem Cach dung (bam dup cung duoc):
rem   scripts\setup.bat                 hoi mat khau root cua MySQL roi lam het
rem   scripts\setup.bat --reset         CSDL da co thi xoa va tao lai khong hoi
rem   scripts\setup.bat --help          xem moi tuy chon
rem Tuy chon co dau "=" phai dat trong ngoac kep, vi du: scripts\setup.bat "--port=3307"
setlocal
chcp 65001 >nul
cd /d "%~dp0.."
call "%~dp0lib\check-jdk.bat" || goto :fail
rem Tieng Viet do chuong trinh Java in ra hien dung dau trong cua so dong lenh.
set "MAVEN_OPTS=-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 %MAVEN_OPTS%"
rem Bo dau ngoac kep quanh tung tuy chon: ca chuoi tuy chon se nam trong mot cap ngoac kep khi chuyen cho Maven.
set "ARGS=%*"
if defined ARGS set "ARGS=%ARGS:"=%"

echo - Build du an (lan dau tai thu vien, co the mat vai phut)
call mvnw.cmd -q compile || goto :fail
call mvnw.cmd -q exec:java@setup "-Dexec.args=%ARGS%" || goto :fail

echo.
echo Tiep theo:
echo   scripts\start-staff.bat     ung dung quan ly (gv.viet / gv123456 hoac admin / admin123)
echo   scripts\start-client.bat    ung dung thi (ma SV viet thuong, vi du b24dcce098 / sv123456)
echo   scripts\start-demo.bat      mo ca hai ung dung cung luc
echo.
pause
exit /b 0

:fail
echo.
echo Chua dung xong. Xem loi o tren; huong dan day du: docs\SETUP.md
pause
exit /b 1
