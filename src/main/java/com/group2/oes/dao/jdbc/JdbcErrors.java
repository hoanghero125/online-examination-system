package com.group2.oes.dao.jdbc;

import com.group2.oes.exception.DataAccessException;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.exception.SessionRuleException;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Tiện ích dùng chung cho các lớp DAO JDBC. */
final class JdbcErrors {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    /** SQLSTATE do trigger trong security.sql phát ra (SIGNAL SQLSTATE '45000') khi chặn máy thí sinh. */
    private static final String SQLSTATE_TRIGGER_REJECTED = "45000";

    private JdbcErrors() {
    }

    /**
     * Đổi SQLException thành ngoại lệ của ứng dụng:
     * - trigger chặn ghi bài đã khóa / quá giờ -> SessionRuleException, như khi tầng service tự phát hiện;
     * - trùng khóa duy nhất -> DuplicateKeyException để service xử lý theo nghiệp vụ;
     * - còn lại -> DataAccessException.
     */
    static ExamAppException translate(SQLException e) {
        if (SQLSTATE_TRIGGER_REJECTED.equals(e.getSQLState())) {
            return new SessionRuleException(SessionRuleException.SUBMISSION_LOCKED,
                    "Bài làm đã nộp hoặc ca thi đã hết giờ, không ghi thêm được câu trả lời.", e);
        }
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
