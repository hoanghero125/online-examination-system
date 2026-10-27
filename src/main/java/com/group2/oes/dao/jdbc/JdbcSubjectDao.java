package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Subject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSubjectDao implements SubjectDao {

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
                    return rs.next()
                            ? Optional.of(new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name")))
                            : Optional.empty();
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
                    list.add(new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name")));
                }
                return list;
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
    public void loadSubjectsOf(Instructor instructor) {
        List<Subject> subjects = cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT s.id, s.code, s.name FROM subject s"
                            + " JOIN instructor_subject i ON i.subject_id = s.id WHERE i.instructor_id = ?")) {
                ps.setInt(1, instructor.getUserId());
                List<Subject> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name")));
                    }
                }
                return list;
            }
        });
        subjects.forEach(instructor::assignSubject);
    }
}
