#!/usr/bin/env bash
# Giống scripts/setup.sh nhưng không cần cài MySQL: tự chạy MySQL 8.0 trong Docker (container oes-mysql,
# mật khẩu root oes_root, cổng 3306 hoặc biến OES_DOCKER_PORT), rồi dựng CSDL như setup.sh.
# Lần sau chỉ cần `docker start oes-mysql` (hoặc chạy lại script này) trước khi mở ứng dụng.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"

CONTAINER=oes-mysql
ROOT_PASSWORD=oes_root
PORT="${OES_DOCKER_PORT:-3306}"

command -v docker >/dev/null || die "Chưa cài Docker (https://docs.docker.com/get-docker/), hoặc dùng scripts/setup.sh với MySQL đã cài."
docker info >/dev/null 2>&1 || die "Docker chưa chạy hoặc tài khoản này chưa có quyền dùng Docker."

if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER"; then
    echo "- Khởi động container $CONTAINER có sẵn"
    docker start "$CONTAINER" >/dev/null
    # Cổng đã cố định từ lúc tạo container: hỏi docker cổng thật, để chạy lại không cần nhớ đặt OES_DOCKER_PORT.
    PORT=$(docker port "$CONTAINER" 3306/tcp | sed -n '1s/.*://p')
else
    echo "- Tạo container $CONTAINER (MySQL 8.0, cổng $PORT)"
    # TZ: MySQL lấy múi giờ của hệ điều hành, mà container mặc định là UTC; không đặt thì NOW() và giờ mở/đóng
    # ca thi hiện chậm 7 tiếng so với đồng hồ thật.
    docker run -d --name "$CONTAINER" -p "$PORT:3306" -e MYSQL_ROOT_PASSWORD="$ROOT_PASSWORD" \
        -e TZ=Asia/Ho_Chi_Minh mysql:8.0 --character-set-server=utf8mb4 >/dev/null
fi

echo -n "- Chờ MySQL sẵn sàng trên cổng $PORT"
# Lần đầu container chạy một MySQL tạm để khởi tạo, không nghe TCP; hỏi qua TCP mới biết MySQL thật đã lên.
ready=false
for _ in $(seq 1 90); do
    if docker exec "$CONTAINER" mysql -uroot -p"$ROOT_PASSWORD" --protocol=TCP -h127.0.0.1 -e "SELECT 1" >/dev/null 2>&1; then
        ready=true
        break
    fi
    echo -n "."
    sleep 2
done
echo
$ready || die "MySQL trong container chưa lên sau 3 phút. Xem: docker logs $CONTAINER"

exec scripts/setup.sh --port="$PORT" --root-password="$ROOT_PASSWORD" "$@"
