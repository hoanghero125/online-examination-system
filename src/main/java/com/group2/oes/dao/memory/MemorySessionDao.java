package com.group2.oes.dao.memory;

import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.model.ExamSession;

import java.time.LocalDateTime;
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
    public LocalDateTime serverTime() {
        return db.now();
    }
}
