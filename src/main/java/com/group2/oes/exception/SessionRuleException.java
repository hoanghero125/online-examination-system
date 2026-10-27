package com.group2.oes.exception;

/** Vi phạm quy tắc của ca thi: vào ngoài giờ, vào lại sau khi nộp, ghi vào bài đã khóa. */
public class SessionRuleException extends ExamAppException {

    private static final long serialVersionUID = 1L;

    public static final String NOT_OPEN_YET = "SESSION_NOT_OPEN_YET";
    public static final String CLOSED = "SESSION_CLOSED";
    public static final String NOT_ENROLLED = "NOT_ENROLLED";
    public static final String ALREADY_SUBMITTED = "ALREADY_SUBMITTED";
    public static final String SUBMISSION_LOCKED = "SUBMISSION_LOCKED";

    public SessionRuleException(String code, String message) {
        super(code, message);
    }

    /** Dùng khi CSDL từ chối ghi (trigger báo SQLSTATE 45000): giữ lỗi gốc để ghi log. */
    public SessionRuleException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /** BR-01: không có tên trong danh sách dự thi. Hai phía (ExamSession, StudentExamService) dùng chung câu báo. */
    public static SessionRuleException notEnrolled() {
        return new SessionRuleException(NOT_ENROLLED,
                "Bạn không có tên trong danh sách dự thi của ca này. Hãy liên hệ giảng viên.");
    }
}
