#!/usr/bin/env bash
# Mở ứng dụng quản lý (StaffApp). Đang mở là máy chấm đang chạy.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
check_jdk
[ -f db-staff.properties ] || echo "Chưa có db-staff.properties: chạy scripts/setup.sh trước (hoặc nhập máy chủ ở màn hình đầu)."
exec ./mvnw -q javafx:run@staff
