package com.group2.oes.ui;

import com.group2.oes.exception.ValidationException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Tìm và ghi tệp cấu hình kết nối CSDL của hai ứng dụng.
 *
 * Thứ tự tìm (Phase 3 của kế hoạch):
 * <ol>
 *   <li>tham số dòng lệnh {@code --config=đường/dẫn.properties};</li>
 *   <li>tệp cùng tên đặt cạnh tệp jar (khi chạy từ IntelliJ/Maven: thư mục đang chạy);</li>
 *   <li>không có thì ứng dụng hiện màn hình nhập địa chỉ máy chủ và lưu lại vào chỗ ở bước 2.</li>
 * </ol>
 */
public final class AppConfig {

    public static final String STAFF_FILE = "db-staff.properties";
    public static final String CLIENT_FILE = "db-client.properties";

    private AppConfig() {
    }

    /** Thông tin nhập ở màn hình cài đặt máy chủ. */
    public record ServerSettings(String host, int port, String database, String user, String password) {

        public ServerSettings {
            ValidationException.require(host != null && !host.isBlank() && !host.strip().contains(" "),
                    "HOST_INVALID", "Hãy nhập địa chỉ máy chủ, ví dụ 192.168.1.10.");
            ValidationException.require(port > 0 && port < 65536, "PORT_INVALID", "Cổng phải từ 1 tới 65535 (MySQL thường là 3306).");
            ValidationException.require(database != null && database.matches("[A-Za-z0-9_]+"), "DB_INVALID",
                    "Tên CSDL chỉ gồm chữ, số và dấu gạch dưới, ví dụ oes.");
            ValidationException.require(user != null && !user.isBlank(), "USER_INVALID", "Hãy nhập tài khoản CSDL.");
            host = host.strip();
            user = user.strip();
        }

        /** Thời gian chờ ghi sẵn trong URL để mất mạng thì báo lỗi sau vài giây, không treo. */
        public String jdbcUrl() {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useUnicode=true&characterEncoding=utf8&connectTimeout=5000&socketTimeout=30000";
        }
    }

    public static Optional<Path> locate(Map<String, String> namedParams, String fileName) {
        String fromArgs = namedParams.get("config");
        if (fromArgs != null && !fromArgs.isBlank()) {
            return Optional.of(Path.of(fromArgs));   // chỉ định rõ thì dùng, kể cả khi chưa có (báo lỗi khi đọc)
        }
        Path beside = defaultLocation(fileName);
        return Files.exists(beside) ? Optional.of(beside) : Optional.empty();
    }

    /**
     * Thư mục chứa tệp jar. Khi chạy từ IntelliJ hoặc Maven, mã nằm trong thư mục target/classes
     * chứ không phải jar, nên dùng thư mục đang chạy (thư mục dự án).
     */
    public static Path defaultLocation(String fileName) {
        try {
            Path code = Path.of(AppConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(code) && code.getParent() != null) {
                return code.getParent().resolve(fileName);
            }
        } catch (URISyntaxException | SecurityException e) {
            // không xác định được vị trí jar: dùng thư mục đang chạy
        }
        return Path.of(fileName).toAbsolutePath();
    }

    public static Path save(Path file, ServerSettings s) throws IOException {
        Properties p = new Properties();
        p.setProperty("db.url", s.jdbcUrl());
        p.setProperty("db.user", s.user());
        p.setProperty("db.password", s.password() == null ? "" : s.password());
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "Cấu hình kết nối CSDL, tạo từ màn hình cài đặt máy chủ");
        }
        return file;
    }
}
