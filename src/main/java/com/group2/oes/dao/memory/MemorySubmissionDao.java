package com.group2.oes.dao.memory;

import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Submission;

import java.util.Comparator;
import java.util.List;

class MemorySubmissionDao implements SubmissionDao {

    private final InMemoryDatabase db;

    MemorySubmissionDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public List<Submission> findAllOfSession(ExamSession session) {
        synchronized (db) {
            return db.submissions.values().stream()
                    .filter(r -> r.sessionId == session.getSessionId())
                    .map(db::toSubmission)
                    .sorted(Comparator.comparing((Submission s) -> s.getStudent().getStudentCode()))
                    .toList();
        }
    }
}
