package com.group2.oes.exception;

/**
 * Gốc chung của mọi ngoại lệ trong ứng dụng.
 *
 * Kế thừa RuntimeException vì ngoại lệ phải đi xuyên qua nhiều tầng
 * (DAO -> service -> view) mà không bắt từng tầng phải khai báo throws.
 * Tầng view chỉ cần bắt đúng một kiểu này.
 */
public abstract class ExamAppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    protected ExamAppException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected ExamAppException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** Mã lỗi ổn định, dùng để ghi log và viết kiểm thử. */
    public String getCode() {
        return code;
    }

    /** Thông điệp hiển thị cho người dùng cuối. */
    public String getUserMessage() {
        return getMessage();
    }
}
