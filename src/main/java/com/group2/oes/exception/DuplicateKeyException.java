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

    /** Không dùng câu "không kết nối được" của lớp cha: kết nối vẫn tốt, chỉ là dữ liệu bị trùng. */
    @Override
    public String getUserMessage() {
        return "Dữ liệu bị trùng với một bản ghi đã có (tên đăng nhập, mã môn, mã lớp...). Hãy kiểm tra lại rồi nhập giá trị khác.";
    }
}
