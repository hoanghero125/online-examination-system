package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.StudentLoginDao;
import com.group2.oes.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/**
 * Đăng nhập phía máy thí sinh, đọc khung nhìn v_login bằng tài khoản oes_client.
 *
 * Viết lớp riêng thay vì cho JdbcUserDao nhận tên bảng/khung nhìn qua constructor:
 * khung nhìn chỉ đủ cột cho việc đăng nhập (không có class_id), và quan trọng hơn,
 * kiểu dữ liệu StudentLoginDao không có hàm insert/update nên ứng dụng thi không thể gọi nhầm.
 */
public class JdbcStudentLoginDao implements StudentLoginDao {

    private final ConnectionManager cm;

    public JdbcStudentLoginDao(ConnectionManager cm) {
        this.cm = cm;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, username, password_hash, full_name, email, role, active, student_code"
                            + " FROM v_login WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    // cùng tên cột với bảng app_user nên dùng lại được hàm dựng lớp con của JdbcUserDao
                    return rs.next() ? Optional.of(JdbcUserDao.mapUser(rs)) : Optional.empty();
                }
            }
        });
    }
}
