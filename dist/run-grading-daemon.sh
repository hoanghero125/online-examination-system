#!/bin/sh
# May cham khong giao dien, chay tren may chu CSDL. Dung bang Ctrl+C
# Can Java 17 tro len (java -version). Chay tu thu muc chua tep jar de tim thay tep cau hinh va ghi nhat ky dung cho.
cd "$(dirname "$0")" || exit 1
exec java -Dfile.encoding=UTF-8 -cp oes-staff.jar com.group2.oes.app.GradingDaemon db-staff.properties "$@"
