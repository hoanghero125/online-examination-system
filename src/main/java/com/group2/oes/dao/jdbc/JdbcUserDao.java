package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.UserDao;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserDao implements UserDao {

    static final String USER_COLUMNS =
            "u.id, u.username, u.password_hash, u.full_name, u.email, u.role, u.active, u.must_change_password,"
                    + " u.student_code";

    /** Thêm lớp học phần của thí sinh (nếu có) để màn hình quản lý người dùng hiện và sửa được lớp. */
    private static final String SELECT_WITH_CLASS =
            "SELECT " + USER_COLUMNS + ", c.id AS c_id, c.code AS c_code, c.semester AS c_semester,"
                    + " s.id AS cs_id, s.code AS cs_code, s.name AS cs_name"
                    + " FROM app_user u LEFT JOIN course_class c ON c.id = u.class_id"
                    + " LEFT JOIN subject s ON s.id = c.subject_id";

    private final ConnectionManager cm;

    public JdbcUserDao(ConnectionManager cm) {
        this.cm = cm;
    }

    /**
     * Dựng đúng lớp con từ một dòng có các cột USER_COLUMNS (bảng app_user hoặc khung nhìn v_login).
     * Dùng chung cho các DAO khác cần nạp thông tin thí sinh (ví dụ khi đọc bảng điểm).
     */
    static User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String hash = rs.getString("password_hash");
        String name = rs.getString("full_name");
        String email = rs.getString("email");
        boolean active = rs.getBoolean("active");
        Role role = Role.valueOf(rs.getString("role"));
        User u = switch (role) {
            case ADMIN -> new Administrator(id, username, hash, name, email, active);
            case INSTRUCTOR -> new Instructor(id, username, hash, name, email, active);
            case STUDENT -> new Student(id, username, hash, name, email, active, rs.getString("student_code"));
        };
        u.setMustChangePassword(rs.getBoolean("must_change_password"));
        return u;
    }

    private static User mapUserWithClass(ResultSet rs) throws SQLException {
        User u = mapUser(rs);
        int classId = rs.getInt("c_id");
        if (!rs.wasNull() && u instanceof Student s) {
            Subject subject = new Subject(rs.getInt("cs_id"), rs.getString("cs_code"), rs.getString("cs_name"));
            s.setCourseClass(new CourseClass(classId, rs.getString("c_code"), subject, rs.getString("c_semester")));
        }
        return u;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findOne(SELECT_WITH_CLASS + " WHERE u.username = ?", username);
    }

    @Override
    public Optional<User> findById(int userId) {
        return findOne(SELECT_WITH_CLASS + " WHERE u.id = ?", userId);
    }

    private Optional<User> findOne(String sql, Object param) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, param);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapUserWithClass(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public <T extends User> T insert(T user) {
        return cm.execute(c -> {
            String sql = "INSERT INTO app_user(username, password_hash, full_name, email, role, active,"
                    + " must_change_password, student_code, class_id) VALUES (?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getRole().name());
                ps.setBoolean(6, user.isActive());
                ps.setBoolean(7, user.isMustChangePassword());
                if (user instanceof Student s) {
                    ps.setString(8, s.getStudentCode());
                    setClassId(ps, 9, s);
                } else {
                    ps.setNull(8, Types.VARCHAR);
                    ps.setNull(9, Types.INTEGER);
                }
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    user.assignId(keys.getInt(1));
                }
                return user;
            }
        });
    }

    /** Tên đăng nhập, vai trò, mã sinh viên không đổi sau khi tạo, nên không có trong câu UPDATE. */
    @Override
    public void update(User user) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE app_user SET password_hash=?, full_name=?, email=?, active=?, must_change_password=?,"
                            + " class_id=? WHERE id=?")) {
                ps.setString(1, user.getPasswordHash());
                ps.setString(2, user.getFullName());
                ps.setString(3, user.getEmail());
                ps.setBoolean(4, user.isActive());
                ps.setBoolean(5, user.isMustChangePassword());
                if (user instanceof Student s) {
                    setClassId(ps, 6, s);
                } else {
                    ps.setNull(6, Types.INTEGER);
                }
                ps.setInt(7, user.getUserId());
                return ps.executeUpdate();
            }
        });
    }

    private static void setClassId(PreparedStatement ps, int index, Student s) throws SQLException {
        if (s.getCourseClass() != null) {
            ps.setInt(index, s.getCourseClass().getClassId());
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    @Override
    public List<Student> findStudentsOfClass(int classId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    SELECT_WITH_CLASS + " WHERE u.class_id = ? AND u.role = 'STUDENT' ORDER BY u.student_code")) {
                ps.setInt(1, classId);
                List<Student> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add((Student) mapUserWithClass(rs));
                    }
                }
                return list;
            }
        });
    }

    @Override
    public List<User> findAll() {
        return search(null, null);
    }

    /** Điều kiện lọc ghép bằng tham số "? IS NULL OR ...", nên chỉ cần một câu SQL cố định cho mọi tổ hợp lọc. */
    @Override
    public List<User> search(String keyword, Role role) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.strip() + "%";
        String roleName = role == null ? null : role.name();
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_WITH_CLASS
                    + " WHERE (? IS NULL OR u.role = ?)"
                    + " AND (? IS NULL OR u.username LIKE ? OR u.full_name LIKE ? OR u.student_code LIKE ?)"
                    + " ORDER BY u.role, u.username")) {
                ps.setString(1, roleName);
                ps.setString(2, roleName);
                for (int i = 3; i <= 6; i++) {
                    ps.setString(i, like);
                }
                List<User> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapUserWithClass(rs));
                    }
                }
                return list;
            }
        });
    }
}
