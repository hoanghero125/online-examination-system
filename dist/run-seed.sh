#!/bin/sh
# Tao du lieu mau cho buoi demo (chay mot lan tren CSDL moi)
# Can Java 17 tro len (java -version). Chay tu thu muc chua tep jar de tim thay tep cau hinh va ghi nhat ky dung cho.
cd "$(dirname "$0")" || exit 1
exec java -Dfile.encoding=UTF-8 -cp oes-staff.jar com.group2.oes.tools.SeedTool db-staff.properties "$@"
