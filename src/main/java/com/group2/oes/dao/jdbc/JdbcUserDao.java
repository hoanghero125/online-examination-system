package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.UserDao;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
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
            "u.id, u.username, u.password_hash, u.full_name, u.email, u.role, u.active, u.student_code";

    private final ConnectionManager cm;

    public JdbcUserDao(ConnectionManager cm) {
        this.cm = cm;
    }

    /**
     * Dựng đúng lớp con từ một dòng của bảng app_user.
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
        return switch (role) {
            case ADMIN -> new Administrator(id, username, hash, name, email, active);
            case INSTRUCTOR -> new Instructor(id, username, hash, name, email, active);
            case STUDENT -> new Student(id, username, hash, name, email, active, rs.getString("student_code"));
        };
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + USER_COLUMNS + " FROM app_user u WHERE u.username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<User> findById(int userId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + USER_COLUMNS + " FROM app_user u WHERE u.id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public <T extends User> T insert(T user) {
        return cm.execute(c -> {
            String sql = "INSERT INTO app_user(username, password_hash, full_name, email, role, active, student_code, class_id)"
                    + " VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getRole().name());
                ps.setBoolean(6, user.isActive());
                if (user instanceof Student s) {
                    ps.setString(7, s.getStudentCode());
                    if (s.getCourseClass() != null) {
                        ps.setInt(8, s.getCourseClass().getClassId());
                    } else {
                        ps.setNull(8, Types.INTEGER);
                    }
                } else {
                    ps.setNull(7, Types.VARCHAR);
                    ps.setNull(8, Types.INTEGER);
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

    @Override
    public void update(User user) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE app_user SET password_hash=?, full_name=?, email=?, active=? WHERE id=?")) {
                ps.setString(1, user.getPasswordHash());
                ps.setString(2, user.getFullName());
                ps.setString(3, user.getEmail());
                ps.setBoolean(4, user.isActive());
                ps.setInt(5, user.getUserId());
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public List<Student> findStudentsOfClass(int classId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + USER_COLUMNS + " FROM app_user u WHERE u.class_id = ? AND u.role = 'STUDENT'"
                            + " ORDER BY u.student_code")) {
                ps.setInt(1, classId);
                List<Student> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add((Student) mapUser(rs));
                    }
                }
                return list;
            }
        });
    }

    @Override
    public List<User> findAll() {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + USER_COLUMNS + " FROM app_user u ORDER BY u.role, u.username");
                 ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
                return list;
            }
        });
    }
}
