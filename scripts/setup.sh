#!/usr/bin/env bash
# Dựng mọi thứ để chạy dự án trên máy này, một lần:
#   kiểm tra JDK -> build -> tạo CSDL oes (schema.sql, security.sql) -> ghi db-staff.properties,
#   db-client.properties -> tạo dữ liệu mẫu (40 sinh viên lớp D24CQCE07-B, 60 câu hỏi, 1 ca thi mở 2 giờ).
# Cần MySQL 8 đang chạy trên máy; chưa có MySQL thì dùng scripts/setup-docker.sh.
#
# Cách dùng:
#   scripts/setup.sh                 hỏi mật khẩu root của MySQL rồi làm hết
#   scripts/setup.sh --reset         CSDL đã có thì xóa và tạo lại không hỏi
#   scripts/setup.sh --help          xem mọi tùy chọn (--port=, --root-password=, --no-seed, ...)
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
check_jdk

echo "- Build dự án (lần đầu tải thư viện, có thể mất vài phút)"
./mvnw -q compile
./mvnw -q exec:java@setup -Dexec.args="$*"

if [[ " $* " != *" --help "* ]]; then
    cat <<'TXT'

Tiếp theo:
  scripts/start-staff.sh     ứng dụng quản lý (gv.viet / gv123456 hoặc admin / admin123)
  scripts/start-client.sh    ứng dụng thi (mã SV viết thường, ví dụ b24dcce098 / sv123456)
  scripts/start-demo.sh      mở cả hai ứng dụng cùng lúc
TXT
fi
