#!/bin/sh
# Ung dung thi (cai tren may thi sinh)
# Can Java 17 tro len (java -version). Chay tu thu muc chua tep jar de tim thay tep cau hinh va ghi nhat ky dung cho.
cd "$(dirname "$0")" || exit 1
exec java -Dfile.encoding=UTF-8 -jar oes-client.jar "$@"
