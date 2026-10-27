package com.group2.oes.dao;

import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    Optional<User> findByUsername(String username);

    Optional<User> findById(int userId);

    /** Thêm người dùng mới, gán mã do CSDL sinh ra vào đối tượng rồi trả lại chính nó. */
    <T extends User> T insert(T user);

    /** Ghi họ tên, email, mật khẩu, trạng thái khóa, cờ buộc đổi mật khẩu; với thí sinh ghi cả lớp học phần. */
    void update(User user);

    List<Student> findStudentsOfClass(int classId);

    List<User> findAll();

    /**
     * Tìm theo tên đăng nhập, họ tên hoặc mã sinh viên (FR-01).
     * @param keyword null hoặc rỗng thì không lọc theo từ khóa
     * @param role    null thì lấy mọi vai trò
     */
    List<User> search(String keyword, Role role);
}
