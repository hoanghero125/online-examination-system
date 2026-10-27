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
}
