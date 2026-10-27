package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Subject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSubjectDao implements SubjectDao {

    private static final String SELECT_CLASS =
            "SELECT c.id, c.code, c.semester, s.id AS s_id, s.code AS s_code, s.name AS s_name"
                    + " FROM course_class c JOIN subject s ON s.id = c.subject_id";

    private final ConnectionManager cm;

    public JdbcSubjectDao(ConnectionManager cm) {
        this.cm = cm;
    }

    @Override
    public Subject insert(String code, String name) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO subject(code, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, code);
                ps.setString(2, name);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    return new Subject(k.getInt(1), code, name);
                }
            }
        });
    }

    @Override
    public Optional<Subject> findByCode(String code) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, code, name FROM subject WHERE code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapSubject(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Subject> findAll() {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, code, name FROM subject ORDER BY code");
                 ResultSet rs = ps.executeQuery()) {
                List<Subject> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapSubject(rs));
                }
                return list;
            }
        });
    }

    private static Subject mapSubject(ResultSet rs) throws SQLException {
        return new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name"));
    }

    @Override
    public void update(Subject subject) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE subject SET name = ? WHERE id = ?")) {
                ps.setString(1, subject.getName());
                ps.setInt(2, subject.getSubjectId());
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public void delete(int subjectId) {
        cm.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM instructor_subject WHERE subject_id = ?")) {
                ps.setInt(1, subjectId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM subject WHERE id = ?")) {
                ps.setInt(1, subjectId);
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public boolean isInUse(int subjectId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM question WHERE subject_id = ?)"
                            + " OR EXISTS(SELECT 1 FROM exam WHERE subject_id = ?)"
                            + " OR EXISTS(SELECT 1 FROM course_class WHERE subject_id = ?)")) {
                for (int i = 1; i <= 3; i++) {
                    ps.setInt(i, subjectId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getBoolean(1);
                }
            }
        });
    }

    @Override
    public CourseClass insertClass(String code, Subject subject, String semester) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO course_class(code, subject_id, semester) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, code);
                ps.setInt(2, subject.getSubjectId());
                ps.setString(3, semester);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    return new CourseClass(k.getInt(1), code, subject, semester);
                }
            }
        });
    }

    @Override
    public void updateClass(CourseClass cls) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE course_class SET code = ?, semester = ? WHERE id = ?")) {
                ps.setString(1, cls.getCode());
                ps.setString(2, cls.getSemester());
                ps.setInt(3, cls.getClassId());
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public Optional<CourseClass> findClassByCode(String code) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_CLASS + " WHERE c.code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapClass(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<CourseClass> findClasses(Subject subject) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    SELECT_CLASS + " WHERE (? IS NULL OR c.subject_id = ?) ORDER BY s.code, c.code")) {
                Integer id = subject == null ? null : subject.getSubjectId();
                ps.setObject(1, id);
                ps.setObject(2, id);
                List<CourseClass> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapClass(rs));
                    }
                }
                return list;
            }
        });
    }

    private static CourseClass mapClass(ResultSet rs) throws SQLException {
        Subject s = new Subject(rs.getInt("s_id"), rs.getString("s_code"), rs.getString("s_name"));
        return new CourseClass(rs.getInt("id"), rs.getString("code"), s, rs.getString("semester"));
    }

    @Override
    public void assignInstructor(Instructor instructor, Subject subject) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO instructor_subject(instructor_id, subject_id) VALUES (?, ?)")) {
                ps.setInt(1, instructor.getUserId());
                ps.setInt(2, subject.getSubjectId());
                return ps.executeUpdate();
            }
        });
        instructor.assignSubject(subject);
    }

    @Override
    public void unassignInstructor(Instructor instructor, Subject subject) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM instructor_subject WHERE instructor_id = ? AND subject_id = ?")) {
                ps.setInt(1, instructor.getUserId());
                ps.setInt(2, subject.getSubjectId());
                return ps.executeUpdate();
            }
        });
        instructor.unassignSubject(subject);
    }

    @Override
    public List<Instructor> findInstructorsOf(Subject subject) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + JdbcUserDao.USER_COLUMNS + " FROM app_user u"
                            + " JOIN instructor_subject i ON i.instructor_id = u.id WHERE i.subject_id = ?"
                            + " ORDER BY u.full_name")) {
                ps.setInt(1, subject.getSubjectId());
                List<Instructor> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add((Instructor) JdbcUserDao.mapUser(rs));
                    }
                }
                return list;
            }
        });
    }

    @Override
    public void loadSubjectsOf(Instructor instructor) {
        List<Subject> subjects = cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT s.id, s.code, s.name FROM subject s"
                            + " JOIN instructor_subject i ON i.subject_id = s.id WHERE i.instructor_id = ?"
                            + " ORDER BY s.code")) {
                ps.setInt(1, instructor.getUserId());
                List<Subject> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapSubject(rs));
                    }
                }
                return list;
            }
        });
        subjects.forEach(instructor::assignSubject);
    }
}
