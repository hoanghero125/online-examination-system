package com.group2.oes.model;

import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bài làm của một thí sinh trong một ca thi. Mỗi thí sinh có đúng một bài làm trong mỗi ca.
 *
 * Vòng đời: IN_PROGRESS -> SUBMITTED -> GRADED.
 * Bài chuyển sang SUBMITTED theo một trong ba lý do (tự nộp, hết giờ, vi phạm đủ 5 lần)
 * rồi được chấm ngay, vì hệ thống chỉ có câu hỏi khách quan.
 */
public class Submission {

    public static final int MAX_VIOLATIONS = 5;

    private static final Logger LOG = Logger.getLogger(Submission.class.getName());

    private int submissionId;
    private final Student student;
    private final ExamSession session;
    private final LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private SubmissionStatus status;
    private EndReason endReason;
    private double totalScore;
    private int violationCount;

    // Khóa là số thứ tự câu trong đề gốc. LinkedHashMap giữ thứ tự thí sinh trả lời.
    private final Map<Integer, Answer> answers = new LinkedHashMap<>();

    /** Bài làm mới, vừa bắt đầu. */
    public Submission(Student student, ExamSession session, LocalDateTime startedAt) {
        this(0, student, session, startedAt, null, SubmissionStatus.IN_PROGRESS, null, 0, 0);
    }

    /** Dựng lại bài làm đã có trong CSDL. */
    public Submission(int submissionId, Student student, ExamSession session,
                      LocalDateTime startedAt, LocalDateTime submittedAt, SubmissionStatus status,
                      EndReason endReason, double totalScore, int violationCount) {
        this.submissionId = submissionId;
        this.student = Objects.requireNonNull(student);
        this.session = Objects.requireNonNull(session);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.submittedAt = submittedAt;
        this.status = Objects.requireNonNull(status);
        this.endReason = endReason;
        this.totalScore = totalScore;
        this.violationCount = violationCount;
    }

    /** BR-04: chỉ ghi được khi bài còn mở và ca thi chưa đóng. */
    public void saveAnswer(int itemOrder, String response, LocalDateTime now) {
        ensureWritable(now);
        ExamItem item = session.getExam().getItem(itemOrder);   // ném ValidationException nếu sai số câu
        answers.computeIfAbsent(itemOrder, k -> new Answer(item, null)).setResponse(response);
    }

    /** Dùng khi DAO nạp lại các câu trả lời đã lưu. */
    public Answer restoreAnswer(int itemOrder, String response) {
        Answer a = new Answer(session.getExam().getItem(itemOrder), response);
        answers.put(itemOrder, a);
        return a;
    }

    /** Tăng bộ đếm vi phạm, trả về số lần hiện tại. Bài đã khóa thì không đếm thêm. */
    public int addViolation() {
        if (!isLocked()) {
            violationCount++;
        }
        return violationCount;
    }

    public boolean reachedViolationLimit() {
        return violationCount >= MAX_VIOLATIONS;
    }

    /**
     * Nộp bài và chấm luôn.
     * Trả về false nếu bài đã được nộp từ trước: yêu cầu nộp lần hai bị bỏ qua,
     * giữ nguyên kết quả lần đầu (UC-16, luồng ngoại lệ 3a).
     */
    public boolean submit(EndReason reason, LocalDateTime now) {
        Objects.requireNonNull(reason);
        if (isLocked()) {
            return false;
        }
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = now;
        this.endReason = reason;
        gradeAll();
        return true;
    }

    /**
     * Chấm mọi câu trong đề, kể cả câu thí sinh bỏ trống (được 0 điểm).
     * Một câu chấm lỗi không được làm hỏng cả bài: bắt ngoại lệ, đánh dấu câu đó, chấm tiếp.
     */
    public void gradeAll() {
        if (status != SubmissionStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ chấm được bài ở trạng thái SUBMITTED, hiện là " + status);
        }
        Exam exam = session.getExam();
        double total = 0;
        for (ExamItem item : exam.getItems()) {
            Answer a = answers.computeIfAbsent(item.getOrder(), k -> new Answer(item, null));
            try {
                a.grade(exam.strategyFor(item.getQuestion()));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Lỗi chấm câu " + item.getOrder() + " của bài " + submissionId, e);
                a.markGradingError();
            }
            total += a.getEarnedPoint();
        }
        this.totalScore = Math.round(total * 100.0) / 100.0;
        this.status = SubmissionStatus.GRADED;
    }

    public boolean isLocked() {
        return status != SubmissionStatus.IN_PROGRESS;
    }

    private void ensureWritable(LocalDateTime now) {
        if (isLocked()) {
            throw new SessionRuleException(SessionRuleException.SUBMISSION_LOCKED,
                    "Bài làm đã nộp, không thể sửa câu trả lời.");
        }
        if (session.isClosed(now)) {
            throw new SessionRuleException(SessionRuleException.SUBMISSION_LOCKED,
                    "Đã hết giờ làm bài.");
        }
    }

    public long answeredCount() {
        return answers.values().stream().filter(Answer::isAnswered).count();
    }

    public long unansweredCount() {
        return session.getExam().getItems().size() - answeredCount();
    }

    /** Điểm quy về thang 10 để xếp loại và vẽ phổ điểm. */
    public double scoreOn10() {
        double max = session.getExam().getTotalPoint();
        ValidationException.require(max > 0, "EXAM_ZERO_POINT", "Tổng điểm đề bằng 0.");
        return totalScore / max * 10.0;
    }

    public void assignId(int id) {
        if (submissionId != 0) {
            throw new IllegalStateException("Bài làm đã có mã " + submissionId);
        }
        this.submissionId = id;
    }

    public void setViolationCount(int count) {
        this.violationCount = count;
    }

    public Collection<Answer> getAnswers() {
        return Collections.unmodifiableCollection(answers.values());
    }

    public Answer getAnswer(int itemOrder) {
        return answers.get(itemOrder);
    }

    public int getSubmissionId() {
        return submissionId;
    }

    public Student getStudent() {
        return student;
    }

    public ExamSession getSession() {
        return session;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public EndReason getEndReason() {
        return endReason;
    }

    public boolean isTerminatedByViolation() {
        return endReason == EndReason.VIOLATION;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public int getViolationCount() {
        return violationCount;
    }
}
