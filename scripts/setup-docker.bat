@echo off
rem Giong scripts\setup.bat nhung khong can cai MySQL: tu chay MySQL 8.0 trong Docker Desktop
rem (container oes-mysql, mat khau root oes_root, cong 3306 hoac bien OES_DOCKER_PORT), roi dung CSDL.
rem Lan sau chi can mo Docker Desktop va chay "docker start oes-mysql" (hoac chay lai script nay).
setlocal
chcp 65001 >nul
cd /d "%~dp0.."
set "CONTAINER=oes-mysql"
set "ROOT_PASSWORD=oes_root"
if not defined OES_DOCKER_PORT set "OES_DOCKER_PORT=3306"

where docker >nul 2>&1 || (echo LOI: Chua cai Docker Desktop, hoac dung scripts\setup.bat voi MySQL da cai. & goto :fail)
docker info >nul 2>&1 || (echo LOI: Docker Desktop chua chay. Mo Docker Desktop roi chay lai. & goto :fail)

rem Hoi thang docker xem container co chua; khong dung "findstr /x" vi docker in dong ket thuc bang LF,
rem findstr /x chi khop dong ket thuc bang CRLF nen luon bao "chua co".
docker container inspect %CONTAINER% >nul 2>&1
if errorlevel 1 (
    echo - Tao container %CONTAINER% ^(MySQL 8.0, cong %OES_DOCKER_PORT%^)
    rem TZ: MySQL lay mui gio cua he dieu hanh, ma container mac dinh la UTC; khong dat thi gio may chu
    rem va gio mo/dong ca thi hien cham 7 tieng so voi dong ho that. MySQL cai thang tren Windows da theo gio may.
    docker run -d --name %CONTAINER% -p %OES_DOCKER_PORT%:3306 -e MYSQL_ROOT_PASSWORD=%ROOT_PASSWORD% -e TZ=Asia/Ho_Chi_Minh mysql:8.0 --character-set-server=utf8mb4 >nul || goto :fail
) else (
    echo - Khoi dong container %CONTAINER% co san
    docker start %CONTAINER% >nul || goto :fail
    rem Cong da co dinh tu luc tao container: hoi docker cong that, de chay lai khong can nho dat OES_DOCKER_PORT.
    for /f "tokens=2 delims=:" %%p in ('docker port %CONTAINER% 3306/tcp ^| findstr /b "0.0.0.0:"') do set "OES_DOCKER_PORT=%%p"
)

echo - Cho MySQL san sang tren cong %OES_DOCKER_PORT% (lan dau mat khoang 30 giay)
rem Lan dau container chay mot MySQL tam de khoi tao, khong nghe TCP; hoi qua TCP moi biet MySQL that da len.
set /a TRIES=0
:wait
docker exec %CONTAINER% mysql -uroot -p%ROOT_PASSWORD% --protocol=TCP -h127.0.0.1 -e "SELECT 1" >nul 2>&1
if not errorlevel 1 goto :ready
set /a TRIES+=1
if %TRIES% GEQ 90 (echo LOI: MySQL trong container chua len sau 3 phut. Xem: docker logs %CONTAINER% & goto :fail)
ping -n 3 127.0.0.1 >nul
goto :wait

:ready
call "%~dp0setup.bat" "--port=%OES_DOCKER_PORT%" "--root-password=%ROOT_PASSWORD%" %*
exit /b %errorlevel%

:fail
pause
exit /b 1
