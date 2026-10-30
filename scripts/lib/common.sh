# Hàm dùng chung cho các script trong scripts/. Không chạy trực tiếp tệp này.

# Về thư mục gốc dự án, nơi có mvnw và các tệp cấu hình db-*.properties.
cd "$(dirname "${BASH_SOURCE[0]}")/../.." || exit 1

die() {
    echo "LỖI: $*" >&2
    exit 1
}

# Cần JDK 17 trở lên (có javac); chỉ có JRE thì Maven không biên dịch được.
check_jdk() {
    local javac
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
        javac="$JAVA_HOME/bin/javac"
    else
        javac="$(command -v javac || true)"
    fi
    [ -n "$javac" ] || die "Chưa có JDK (không tìm thấy javac). Cài JDK 17 trở lên, ví dụ Eclipse Temurin 17: https://adoptium.net"
    local version major
    version="$("$javac" -version 2>&1 | awk '{print $2}')"
    major="${version%%.*}"
    [ "$major" -ge 17 ] 2>/dev/null || die "Cần JDK 17 trở lên, máy đang có JDK $version."
}

# Lấy một khóa trong tệp .properties (dùng để giữ nguyên mật khẩu đã đặt khi dựng CSDL kiểm thử).
read_property() {
    local file="$1" key="$2"
    [ -f "$file" ] && sed -n "s/^${key//./\\.}=//p" "$file" | head -n 1
}
