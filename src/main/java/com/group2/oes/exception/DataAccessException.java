package com.group2.oes.exception;

/**
 * Bọc lỗi kỹ thuật khi truy cập cơ sở dữ liệu (thường là SQLException).
 *
 * Nhờ lớp này, tầng service và view không phải import gì từ java.sql.
 * Nguyên nhân gốc vẫn được giữ trong getCause() để ghi log.
 */
public class DataAccessException extends ExamAppException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super("DATA_ACCESS", message, cause);
    }

    /** Người dùng không cần biết câu SQL nào lỗi, chỉ cần biết phải thử lại. */
    @Override
    public String getUserMessage() {
        return "Không kết nối được tới máy chủ dữ liệu. Vui lòng thử lại hoặc báo giám thị.";
    }
}
