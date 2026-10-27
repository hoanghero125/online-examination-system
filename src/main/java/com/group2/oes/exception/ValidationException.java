package com.group2.oes.exception;

/** Dữ liệu đầu vào không hợp lệ. */
public class ValidationException extends ExamAppException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String code, String message) {
        super(code, message);
    }

    /** Ném ra nếu điều kiện sai. Giúp các hàm kiểm tra viết gọn trên một dòng. */
    public static void require(boolean condition, String code, String message) {
        if (!condition) {
            throw new ValidationException(code, message);
        }
    }
}
