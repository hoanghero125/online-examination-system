package com.group2.oes.dao.memory;

import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamPaper;
import com.group2.oes.client.PaperItem;
import com.group2.oes.client.PaperOption;
import com.group2.oes.client.ReviewItem;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.dao.StudentExamDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.TrueFalseQuestion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Bản bộ nhớ của StudentExamDao. Làm đúng như khung nhìn và trigger trong security.sql,
 * để kiểm thử nghiệp vụ không cần MySQL vẫn kiểm tra được BR-04 và NFR-04:
 * - đề trả về không có cờ đúng/sai, chỉ đọc được khi đề đã có ca tới giờ mở (v_exam_paper);
 * - không ghi câu trả lời khi bài đã khóa hoặc ca đã đóng (trigger trg_answer_client_*);
 * - đáp án đúng chỉ có khi ca đã đóng và cho xem đáp án (v_review).
 */
class MemoryStudentExamDao implements StudentExamDao {

    private final InMemoryDatabase db;

    MemoryStudentExamDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<SessionInfo> findSession(int sessionId) {
        synchronized (db) {
            return Optional.ofNullable(db.sessions.get(sessionId)).map(MemoryStudentExamDao::toInfo);
        }
    }

    @Override
    public List<SessionInfo> findSessionsOf(int studentId) {
        synchronized (db) {
            return db.sessions.values().stream()
                    .filter(s -> s.getEnrolledStudentIds().contains(studentId))
                    .sorted(Comparator.comparing(s -> s.getWindow().openTime()))
                    .map(MemoryStudentExamDao::toInfo)
                    .toList();
        }
    }

    private static SessionInfo toInfo(ExamSession s) {
        Exam e = s.getExam();
        return new SessionInfo(s.getSessionId(), e.getExamId(), e.getTitle(), e.getSubject().getCode(),
                e.getSubject().getName(), s.getWindow(), s.getPolicy(), e.getTotalPoint(), e.getItems().size());
    }

    @Override
    public boolean isEnrolled(int sessionId, int studentId) {
        synchronized (db) {
            ExamSession s = db.sessions.get(sessionId);
            return s != null && s.getEnrolledStudentIds().contains(studentId);
        }
    }

    @Override
    public ExamPaper loadPaper(int examId) {
        synchronized (db) {
            LocalDateTime now = db.now();
            boolean visible = db.sessions.values().stream()
                    .anyMatch(s -> s.getExam().getExamId() == examId && !s.getWindow().isUpcoming(now));
            Exam exam = db.exams.get(examId);
            if (!visible || exam == null) {
                return new ExamPaper(List.of());
            }
            return new ExamPaper(exam.getItems().stream().map(MemoryStudentExamDao::toPaperItem).toList());
        }
    }

    /** Chép sang dạng không có đáp án: chỉ mã và nội dung phương án. */
    private static PaperItem toPaperItem(ExamItem item) {
        Question q = item.getQuestion();
        List<PaperOption> options = switch (q.getType()) {
            case SINGLE_CHOICE, MULTI_CHOICE -> q.getOptions().stream()
                    .map(o -> new PaperOption(o.getOptionId(), o.getContent()))
                    .toList();
            case TRUE_FALSE, FILL_IN_BLANK -> List.of();
        };
        return new PaperItem(item.getOrder(), item.getPoint(), q.getQuestionId(), q.getType(), q.getContent(), options);
    }

    @Override
    public Optional<AttemptState> findAttempt(int sessionId, int studentId) {
        synchronized (db) {
            return db.findRow(sessionId, studentId).map(MemoryStudentExamDao::toState);
        }
    }

    private static AttemptState toState(SubmissionRow r) {
        Double score = r.status == SubmissionStatus.GRADED ? r.totalScore : null;
        AttemptState state = new AttemptState(r.id, r.sessionId, r.studentId, r.status, r.violationCount,
                r.endReason, score);
        r.answers.forEach((order, a) -> {
            state.recordAnswer(order, a.response);
            state.setFlag(order, a.flagged);
        });
        return state;
    }

    @Override
    public AttemptState createAttempt(int sessionId, int studentId, LocalDateTime now) {
        synchronized (db) {
            if (db.findRow(sessionId, studentId).isPresent()) {
                throw new DuplicateKeyException("Thí sinh đã có bài trong ca thi này", null);
            }
            SubmissionRow r = new SubmissionRow(db.nextId(), sessionId, studentId, now);
            db.submissions.put(r.id, r);
            return toState(r);
        }
    }

    @Override
    public void saveAnswer(int submissionId, int order, String response) {
        synchronized (db) {
            writableRow(submissionId).answer(order).response = response;
        }
    }

    @Override
    public void setFlag(int submissionId, int order, boolean flagged) {
        synchronized (db) {
            writableRow(submissionId).answer(order).flagged = flagged;
        }
    }

    /** Làm việc của trigger trg_answer_client_insert / _update. */
    private SubmissionRow writableRow(int submissionId) {
        SubmissionRow r = db.submissions.get(submissionId);
        ExamSession session = r == null ? null : db.sessions.get(r.sessionId);
        if (r == null || r.status != SubmissionStatus.IN_PROGRESS || session.getWindow().isClosed(db.now())) {
            throw new SessionRuleException(SessionRuleException.SUBMISSION_LOCKED,
                    "Bài làm đã nộp hoặc ca thi đã hết giờ, không ghi thêm được câu trả lời.");
        }
        return r;
    }

    @Override
    public int increaseViolation(int submissionId) {
        synchronized (db) {
            SubmissionRow r = db.submissions.get(submissionId);
            if (r.status == SubmissionStatus.IN_PROGRESS) {
                r.violationCount++;
            }
            return r.violationCount;
        }
    }

    @Override
    public boolean markSubmitted(int submissionId, EndReason reason, LocalDateTime now) {
        synchronized (db) {
            SubmissionRow r = db.submissions.get(submissionId);
            if (r.status != SubmissionStatus.IN_PROGRESS) {
                return false;
            }
            r.status = SubmissionStatus.SUBMITTED;
            r.submittedAt = now;
            r.endReason = reason;
            return true;
        }
    }

    @Override
    public List<ReviewItem> loadReview(int submissionId) {
        synchronized (db) {
            SubmissionRow r = db.submissions.get(submissionId);
            if (r == null) {
                return List.of();
            }
            ExamSession session = db.sessions.get(r.sessionId);
            boolean showCorrect = session.getPolicy().showAnswer() && session.getWindow().isClosed(db.now());
            List<ReviewItem> items = new ArrayList<>();
            for (ExamItem item : session.getExam().getItems()) {
                PaperItem p = toPaperItem(item);
                SubmissionRow.AnswerRow a = r.answers.get(item.getOrder());
                items.add(new ReviewItem(p.order(), p.point(), p.type(), p.content(), p.options(),
                        a == null ? null : a.response,
                        a == null ? null : a.earnedPoint,
                        showCorrect ? correctAnswerOf(item.getQuestion()) : null));
            }
            return items;
        }
    }

    /** Đáp án đúng theo cùng định dạng câu trả lời, như JdbcStudentExamDao ghép từ v_review. */
    private static String correctAnswerOf(Question q) {
        return switch (q.getType()) {
            case SINGLE_CHOICE, MULTI_CHOICE -> q.getOptions().stream()
                    .filter(AnswerOption::isCorrect)
                    .map(o -> String.valueOf(o.getOptionId()))
                    .collect(Collectors.joining(","));
            case TRUE_FALSE -> String.valueOf(((TrueFalseQuestion) q).getCorrectValue());
            case FILL_IN_BLANK -> String.join(" / ", ((FillInBlankQuestion) q).getAccepted());
        };
    }

    @Override
    public LocalDateTime serverTime() {
        return db.now();
    }
}
