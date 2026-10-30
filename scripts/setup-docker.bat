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

docker ps -a --format "{{.Names}}" | findstr /x "%CONTAINER%" >nul
if errorlevel 1 (
    echo - Tao container %CONTAINER% ^(MySQL 8.0, cong %OES_DOCKER_PORT%^)
    docker run -d --name %CONTAINER% -p %OES_DOCKER_PORT%:3306 -e MYSQL_ROOT_PASSWORD=%ROOT_PASSWORD% mysql:8.0 --character-set-server=utf8mb4 >nul || goto :fail
) else (
    echo - Khoi dong container %CONTAINER% co san
    docker start %CONTAINER% >nul || goto :fail
)

echo - Cho MySQL san sang (lan dau mat khoang 30 giay)
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
