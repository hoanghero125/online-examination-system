package com.group2.oes.dao.memory;

import com.group2.oes.dao.UserDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

class MemoryUserDao implements UserDao {

    private final InMemoryDatabase db;

    MemoryUserDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        synchronized (db) {
            return db.users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }
    }

    @Override
    public Optional<User> findById(int userId) {
        synchronized (db) {
            return Optional.ofNullable(db.users.get(userId));
        }
    }

    @Override
    public <T extends User> T insert(T user) {
        synchronized (db) {
            if (findByUsername(user.getUsername()).isPresent()) {
                throw new DuplicateKeyException("Tên đăng nhập đã tồn tại: " + user.getUsername(), null);
            }
            // mô phỏng ràng buộc UNIQUE(student_code) của bảng app_user
            if (user instanceof Student s && db.users.values().stream()
                    .anyMatch(u -> u instanceof Student o && o.getStudentCode().equals(s.getStudentCode()))) {
                throw new DuplicateKeyException("Mã sinh viên đã tồn tại: " + s.getStudentCode(), null);
            }
            user.assignId(db.nextId());
            db.users.put(user.getUserId(), user);
            if (user instanceof Student s && s.getCourseClass() != null) {
                db.studentClass.put(s.getUserId(), s.getCourseClass().getClassId());
            }
            return user;
        }
    }

    @Override
    public void update(User user) {
        synchronized (db) {
            db.users.put(user.getUserId(), user);
            if (user instanceof Student s) {
                if (s.getCourseClass() == null) {
                    db.studentClass.remove(s.getUserId());
                } else {
                    db.studentClass.put(s.getUserId(), s.getCourseClass().getClassId());
                }
            }
        }
    }

    @Override
    public List<Student> findStudentsOfClass(int classId) {
        synchronized (db) {
            return db.users.values().stream()
                    .filter(u -> u instanceof Student && Objects.equals(db.studentClass.get(u.getUserId()), classId))
                    .map(u -> (Student) u)
                    .sorted(Comparator.comparing(Student::getStudentCode))
                    .toList();
        }
    }

    @Override
    public List<User> findAll() {
        synchronized (db) {
            return new ArrayList<>(db.users.values());
        }
    }

    @Override
    public List<User> search(String keyword, Role role) {
        String k = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        synchronized (db) {
            return db.users.values().stream()
                    .filter(u -> role == null || u.getRole() == role)
                    .filter(u -> k.isEmpty() || contains(u.getUsername(), k) || contains(u.getFullName(), k)
                            || (u instanceof Student s && contains(s.getStudentCode(), k)))
                    .sorted(Comparator.comparing(User::getRole).thenComparing(User::getUsername))
                    .toList();
        }
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
