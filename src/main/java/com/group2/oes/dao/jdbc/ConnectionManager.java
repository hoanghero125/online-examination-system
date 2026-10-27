package com.group2.oes.dao.jdbc;

import com.group2.oes.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * Cấp kết nối JDBC tới MySQL dùng chung của phòng máy.
 * Mỗi thao tác mở một kết nối và đóng ngay bằng try-with-resources;
 * với vài chục máy trạm thì chưa cần tới connection pool.
 */
public class ConnectionManager {

    /** Giống java.util.function.Function nhưng được phép ném SQLException. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    static final int CONNECT_TIMEOUT_MS = 5_000;
    static final int SOCKET_TIMEOUT_MS = 30_000;

    private final String url;
    private final String user;
    private final String password;

    public ConnectionManager(String url, String user, String password) {
        this.url = Objects.requireNonNull(url, "db.url");
        this.user = user;
        this.password = password;
    }

    /** Đọc cấu hình từ tệp db.properties. Thiếu tệp hoặc thiếu khóa thì báo lỗi rõ ràng. */
    public static ConnectionManager fromFile(Path file) {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Không đọc được tệp cấu hình " + file.toAbsolutePath(), e);
        }
        String url = p.getProperty("db.url");
        if (url == null || url.isBlank()) {
            throw new DataAccessException("Tệp " + file + " thiếu khóa db.url", null);
        }
        return new ConnectionManager(url, p.getProperty("db.user"), p.getProperty("db.password"));
    }

    /**
     * Mở kết nối mới. Mặc định của driver là chờ kết nối vô hạn: nếu máy chủ trong phòng máy đang bật nhưng
     * MySQL không nghe cổng, ứng dụng sẽ treo khoảng 2 phút. Vì vậy đặt sẵn thời gian chờ
     * (trừ khi URL đã tự ghi), để mất mạng thì báo lỗi sau vài giây.
     */
    public Connection open() {
        Properties p = new Properties();
        if (user != null) {
            p.setProperty("user", user);
        }
        if (password != null) {
            p.setProperty("password", password);
        }
        if (!url.contains("connectTimeout")) {
            p.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        }
        if (!url.contains("socketTimeout")) {
            p.setProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_MS));
        }
        try {
            return DriverManager.getConnection(url, p);
        } catch (SQLException e) {
            throw new DataAccessException("Không mở được kết nối tới " + url, e);
        }
    }

    /** Chạy một thao tác đơn lẻ, tự đóng kết nối, đổi SQLException thành ngoại lệ của ứng dụng. */
    public <T> T execute(SqlWork<T> work) {
        try (Connection c = open()) {
            return work.run(c);
        } catch (SQLException e) {
            throw JdbcErrors.translate(e);
        }
    }

    /**
     * Chạy nhiều câu lệnh trong một giao dịch: hoặc tất cả cùng được ghi, hoặc không câu nào.
     * Bất kỳ ngoại lệ nào (kể cả ngoại lệ nghiệp vụ ném giữa chừng) đều dẫn tới rollback.
     */
    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(c, e);
                throw e;
            }
        } catch (SQLException e) {
            throw JdbcErrors.translate(e);
        }
    }

    private static void rollbackQuietly(Connection c, Exception original) {
        try {
            c.rollback();
        } catch (SQLException rollbackError) {
            // không che mất lỗi gốc: gắn lỗi rollback vào làm lỗi phụ
            original.addSuppressed(rollbackError);
        }
    }
}
