#!/usr/bin/env bash
# Mở cả ứng dụng quản lý và ứng dụng thi trên cùng máy, để thử hoặc demo một mình.
# Đóng cửa sổ dòng lệnh này (Ctrl+C) thì cả hai ứng dụng cùng tắt.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
check_jdk
# Build một lần trước, để hai lệnh chạy song song không cùng biên dịch vào target/.
./mvnw -q compile
./mvnw -q javafx:run@staff &
staff=$!
# Maven chạy ứng dụng trong một tiến trình java con: tắt cả con lẫn Maven.
trap 'pkill -P $staff 2>/dev/null; kill $staff 2>/dev/null' EXIT
sleep 3
./mvnw -q javafx:run@client
