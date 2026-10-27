package com.group2.oes.dao.memory;

import com.group2.oes.dao.UserDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
}
