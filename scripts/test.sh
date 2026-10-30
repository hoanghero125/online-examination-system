#!/usr/bin/env bash
# Chạy kiểm thử.
#   scripts/test.sh          kiểm thử không cần MySQL (DAO trong bộ nhớ)
#   scripts/test.sh --db     thêm kiểm thử với MySQL thật: dựng CSDL riêng oes_test (xóa và tạo lại mỗi lần),
#                            ghi test-staff.properties, test-client.properties rồi chạy toàn bộ.
# Tùy chọn khác sau --db (ví dụ --port=3307) chuyển cho DatabaseSetup.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
check_jdk

if [ "${1:-}" != "--db" ]; then
    exec ./mvnw test
fi
shift

# oes_staff, oes_client dùng chung cho mọi CSDL trên máy: giữ đúng mật khẩu đã ghi trong cấu hình hiện có,
# để dựng CSDL kiểm thử không làm hỏng db-staff.properties, db-client.properties.
staff_pw="$(read_property db-staff.properties db.password)"
client_pw="$(read_property db-client.properties db.password)"
extra=()
[ -n "$staff_pw" ] && extra+=("--staff-password=$staff_pw")
[ -n "$client_pw" ] && extra+=("--client-password=$client_pw")

./mvnw -q compile
./mvnw -q exec:java@setup -Dexec.args="--database=oes_test --reset --no-seed --staff-config=test-staff.properties --client-config=test-client.properties ${extra[*]:-} $*"
OES_TEST_DB=test-staff.properties OES_TEST_DB_CLIENT=test-client.properties exec ./mvnw test
