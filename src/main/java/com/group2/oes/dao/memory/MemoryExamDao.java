package com.group2.oes.dao.memory;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.model.Exam;

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
    public void lock(int examId) {
        synchronized (db) {
            Exam e = db.exams.get(examId);
            if (e != null && !e.isLocked()) {
                e.lock();
            }
        }
    }
}
