#!/usr/bin/env bash
# Máy chấm không giao diện, dùng khi không mở StaffApp trong giờ thi. Ctrl+C để dừng.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
check_jdk
[ -f db-staff.properties ] || die "Chưa có db-staff.properties: chạy scripts/setup.sh trước."
./mvnw -q compile
exec ./mvnw -q exec:java@grading -Dexec.args="db-staff.properties"
