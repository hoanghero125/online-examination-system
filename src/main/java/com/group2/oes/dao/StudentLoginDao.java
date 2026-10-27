package com.group2.oes.dao;

import com.group2.oes.model.User;

import java.util.Optional;

/**
 * Đăng nhập phía máy thí sinh, đọc qua khung nhìn v_login.
 *
 * Tách khỏi UserDao (ISP): máy thí sinh chỉ cần tra một tài khoản theo tên đăng nhập và tự đổi mật khẩu.
 * Nhờ vậy ứng dụng thi không thể gọi nhầm insert/update/findAll trên bảng người dùng, dù chỉ do lỗi lập trình.
 */
public interface StudentLoginDao {

    /**
     * Tài khoản theo tên đăng nhập. Với tài khoản không phải thí sinh, mã băm mật khẩu là chuỗi rỗng
     * (khung nhìn không trả mã băm của giảng viên, quản trị viên).
     */
    Optional<User> findByUsername(String username);

    /**
     * Đổi mật khẩu của chính thí sinh, đồng thời bỏ cờ buộc đổi mật khẩu.
     * Chỉ ghi khi mã băm hiện tại trong CSDL vẫn là {@code oldHash} (so sánh rồi mới ghi).
     * @return false nếu không đổi được (mã băm đã bị đổi ở máy khác, hoặc không phải tài khoản thí sinh)
     */
    boolean changePassword(int userId, String oldHash, String newHash);
}
