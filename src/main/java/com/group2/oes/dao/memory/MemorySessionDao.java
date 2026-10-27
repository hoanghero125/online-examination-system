package com.group2.oes.dao.memory;

import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.model.ExamSession;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class MemorySessionDao implements ExamSessionDao {

    private final InMemoryDatabase db;

    MemorySessionDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public ExamSession insert(ExamSession s) {
        synchronized (db) {
            s.assignId(db.nextId());
            db.sessions.put(s.getSessionId(), s);
            return s;
        }
    }

    @Override
    public Optional<ExamSession> findById(int sessionId) {
        synchronized (db) {
            return Optional.ofNullable(db.sessions.get(sessionId));
        }
    }

    @Override
    public List<ExamSession> findByExam(int examId) {
        synchronized (db) {
            return db.sessions.values().stream()
                    .filter(s -> s.getExam().getExamId() == examId)
                    .sorted(Comparator.comparing((ExamSession s) -> s.getWindow().openTime()).reversed())
                    .toList();
        }
    }

    @Override
    public LocalDateTime serverTime() {
        return db.now();
    }
}
