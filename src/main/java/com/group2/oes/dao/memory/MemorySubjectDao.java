package com.group2.oes.dao.memory;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Subject;

import java.util.ArrayList;
import java.util.Comparator;
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
            return db.subjects.values().stream().sorted(Comparator.comparing(Subject::getCode)).toList();
        }
    }

    @Override
    public void update(Subject subject) {
        synchronized (db) {
            db.subjects.put(subject.getCode(), subject);
        }
    }

    @Override
    public void delete(int subjectId) {
        synchronized (db) {
            db.subjects.values().removeIf(s -> s.getSubjectId() == subjectId);
            db.instructorSubjects.values().forEach(set -> set.removeIf(s -> s.getSubjectId() == subjectId));
        }
    }

    @Override
    public boolean isInUse(int subjectId) {
        synchronized (db) {
            return db.questions.values().stream().anyMatch(q -> q.getSubject().getSubjectId() == subjectId)
                    || db.exams.values().stream().anyMatch(e -> e.getSubject().getSubjectId() == subjectId)
                    || db.classes.values().stream().anyMatch(c -> c.getSubject().getSubjectId() == subjectId);
        }
    }

    @Override
    public CourseClass insertClass(String code, Subject subject, String semester) {
        synchronized (db) {
            if (findClassByCode(code).isPresent()) {
                throw new DuplicateKeyException("Mã lớp đã tồn tại: " + code, null);
            }
            CourseClass c = new CourseClass(db.nextId(), code, subject, semester);
            db.classes.put(c.getClassId(), c);
            return c;
        }
    }

    @Override
    public void updateClass(CourseClass courseClass) {
        synchronized (db) {
            db.classes.put(courseClass.getClassId(), courseClass);
        }
    }

    @Override
    public Optional<CourseClass> findClassByCode(String code) {
        synchronized (db) {
            return db.classes.values().stream().filter(c -> c.getCode().equals(code)).findFirst();
        }
    }

    @Override
    public List<CourseClass> findClasses(Subject subject) {
        synchronized (db) {
            return db.classes.values().stream()
                    .filter(c -> subject == null || c.getSubject().equals(subject))
                    .sorted(Comparator.comparing((CourseClass c) -> c.getSubject().getCode())
                            .thenComparing(CourseClass::getCode))
                    .toList();
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
    public void unassignInstructor(Instructor instructor, Subject subject) {
        synchronized (db) {
            db.instructorSubjects.getOrDefault(instructor.getUserId(), new HashSet<>()).remove(subject);
            instructor.unassignSubject(subject);
        }
    }

    @Override
    public List<Instructor> findInstructorsOf(Subject subject) {
        synchronized (db) {
            List<Instructor> list = new ArrayList<>();
            db.instructorSubjects.forEach((id, set) -> {
                if (set.contains(subject)) {
                    list.add((Instructor) db.users.get(id));
                }
            });
            return list;
        }
    }

    @Override
    public void loadSubjectsOf(Instructor instructor) {
        synchronized (db) {
            db.instructorSubjects.getOrDefault(instructor.getUserId(), Set.of()).forEach(instructor::assignSubject);
        }
    }
}
