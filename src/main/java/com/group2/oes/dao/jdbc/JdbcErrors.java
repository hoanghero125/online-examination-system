package com.group2.oes.dao.jdbc;

import com.group2.oes.exception.DataAccessException;
import com.group2.oes.exception.DuplicateKeyException;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Tiện ích dùng chung cho các lớp DAO JDBC. */
final class JdbcErrors {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private JdbcErrors() {
    }

    /** Đổi SQLException thành ngoại lệ của ứng dụng, tách riêng trường hợp trùng khóa. */
    static DataAccessException translate(SQLException e) {
        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            return new DuplicateKeyException("Bản ghi bị trùng: " + e.getMessage(), e);
        }
        if (e instanceof SQLIntegrityConstraintViolationException) {
            return new DataAccessException("Vi phạm ràng buộc dữ liệu: " + e.getMessage(), e);
        }
        return new DataAccessException("Lỗi truy cập CSDL: " + e.getMessage(), e);
    }

    static Timestamp ts(LocalDateTime t) {
        return t == null ? null : Timestamp.valueOf(t);
    }

    static LocalDateTime ldt(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
