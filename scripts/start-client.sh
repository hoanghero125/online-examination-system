#!/usr/bin/env bash
# Mở ứng dụng thi (ExamClientApp). Chưa có db-client.properties thì ứng dụng hỏi địa chỉ máy chủ.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
check_jdk
exec ./mvnw -q javafx:run@client
