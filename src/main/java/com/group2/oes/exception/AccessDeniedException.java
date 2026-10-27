package com.group2.oes.exception;

/** Người dùng thao tác ngoài quyền hạn của mình. */
public class AccessDeniedException extends ExamAppException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String code, String message) {
        super(code, message);
    }
}
