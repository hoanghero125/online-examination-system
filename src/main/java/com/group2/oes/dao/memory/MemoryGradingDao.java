package com.group2.oes.dao.memory;

import com.group2.oes.dao.GradingDao;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

class MemoryGradingDao implements GradingDao {

    private final InMemoryDatabase db;

    MemoryGradingDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public int closeExpired(LocalDateTime now) {
        synchronized (db) {
            int count = 0;
            for (SubmissionRow r : db.submissions.values()) {
                ExamSession session = db.sessions.get(r.sessionId);
                if (r.status == SubmissionStatus.IN_PROGRESS && session.getWindow().isClosed(now)) {
                    r.status = SubmissionStatus.SUBMITTED;
                    r.submittedAt = session.getWindow().closeTime();
                    r.endReason = EndReason.TIME_OUT;
                    count++;
                }
            }
            return count;
        }
    }

    @Override
    public List<Integer> findSubmittedIds() {
        synchronized (db) {
            return db.submissions.values().stream()
                    .filter(r -> r.status == SubmissionStatus.SUBMITTED)
                    .sorted(Comparator.comparing((SubmissionRow r) -> r.submittedAt).thenComparing(r -> r.id))
                    .map(r -> r.id)
                    .toList();
        }
    }

    @Override
    public Submission load(int submissionId) {
        synchronized (db) {
            SubmissionRow r = db.submissions.get(submissionId);
            if (r == null) {
                throw new DataAccessException("Không có bài làm " + submissionId, null);
            }
            return db.toSubmission(r);
        }
    }

    @Override
    public boolean saveGrades(Submission s) {
        synchronized (db) {
            SubmissionRow r = db.submissions.get(s.getSubmissionId());
            if (r.status != SubmissionStatus.SUBMITTED) {
                return false;
            }
            r.status = s.getStatus();
            r.totalScore = s.getTotalScore();
            for (ExamItem item : s.getSession().getExam().getItems()) {
                r.answer(item.getOrder()).earnedPoint = s.answerFor(item).getEarnedPoint();
            }
            return true;
        }
    }
}
