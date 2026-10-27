package com.group2.oes.dao.memory;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.model.Exam;
import com.group2.oes.model.Subject;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class MemoryExamDao implements ExamDao {

    private final InMemoryDatabase db;

    MemoryExamDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public Exam insert(Exam exam) {
        synchronized (db) {
            exam.assignId(db.nextId());
            db.exams.put(exam.getExamId(), exam);
            return exam;
        }
    }

    @Override
    public Optional<Exam> findById(int examId) {
        synchronized (db) {
            return Optional.ofNullable(db.exams.get(examId));
        }
    }

    @Override
    public List<Exam> findBySubject(Subject subject) {
        synchronized (db) {
            return db.exams.values().stream()
                    .filter(e -> e.getSubject().equals(subject))
                    .sorted(Comparator.comparing(Exam::getTitle).thenComparingInt(Exam::getVersion))
                    .toList();
        }
    }

    @Override
    public void lock(int examId) {
        synchronized (db) {
            Exam e = db.exams.get(examId);
            if (e != null && !e.isLocked()) {
                e.lock();
            }
        }
    }
}
