package com.group2.oes.dao.memory;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Subject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class MemorySubjectDao implements SubjectDao {

    private final InMemoryDatabase db;

    MemorySubjectDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public Subject insert(String code, String name) {
        synchronized (db) {
            if (db.subjects.containsKey(code)) {
                throw new DuplicateKeyException("Mã môn đã tồn tại: " + code, null);
            }
            Subject s = new Subject(db.nextId(), code, name);
            db.subjects.put(code, s);
            return s;
        }
    }

    @Override
    public Optional<Subject> findByCode(String code) {
        synchronized (db) {
            return Optional.ofNullable(db.subjects.get(code));
        }
    }

    @Override
    public List<Subject> findAll() {
        synchronized (db) {
            return new ArrayList<>(db.subjects.values());
        }
    }

    @Override
    public CourseClass insertClass(String code, Subject subject, String semester) {
        synchronized (db) {
            return new CourseClass(db.nextId(), code, subject, semester);
        }
    }

    @Override
    public void assignInstructor(Instructor instructor, Subject subject) {
        synchronized (db) {
            db.instructorSubjects.computeIfAbsent(instructor.getUserId(), k -> new HashSet<>()).add(subject);
            instructor.assignSubject(subject);
        }
    }

    @Override
    public void loadSubjectsOf(Instructor instructor) {
        synchronized (db) {
            db.instructorSubjects.getOrDefault(instructor.getUserId(), Set.of()).forEach(instructor::assignSubject);
        }
    }
}
