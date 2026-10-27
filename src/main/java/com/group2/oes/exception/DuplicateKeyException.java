package com.group2.oes.exception;

/**
 * Bản ghi bị trùng khóa duy nhất trong CSDL.
 * Tách riêng khỏi DataAccessException vì tầng service xử lý trường hợp này theo nghiệp vụ,
 * ví dụ thí sinh bấm "Vào thi" ở hai máy cùng lúc thì máy sau mở lại bài của máy trước.
 */
public class DuplicateKeyException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
