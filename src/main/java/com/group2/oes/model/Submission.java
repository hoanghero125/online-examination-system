package com.group2.oes.model;

import com.group2.oes.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Bài làm của một thí sinh trong một ca thi, nhìn từ phía giảng viên (chấm điểm, thống kê).
 *
 * Vòng đời: IN_PROGRESS -> SUBMITTED -> GRADED.
 * Từ Milestone 5 lớp này chỉ giữ dữ liệu và kiểm soát chuyển trạng thái hợp lệ (SRP):
 * - ghi câu trả lời, đếm vi phạm: phía thí sinh (StudentExamService, ProctoringService + ViolationPolicy);
 * - chấm điểm: SubmissionGrader, chỉ chạy trên máy giảng viên.
 */
public class Submission {

    private int submissionId;
    private final Student student;
    private final ExamSession session;
    private final LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private SubmissionStatus status;
    private EndReason endReason;
    private double totalScore;
    private final int violationCount;

    // Khóa là số thứ tự câu trong đề gốc; TreeMap để duyệt theo đúng thứ tự câu.
    private final Map<Integer, Answer> answers = new TreeMap<>();

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

    /** Dùng khi DAO nạp lại các câu trả lời đã lưu. */
    public Answer restoreAnswer(int itemOrder, String response) {
        Answer a = new Answer(session.getExam().getItem(itemOrder), response);
        answers.put(itemOrder, a);
        return a;
    }

    /** Câu trả lời của một câu; câu thí sinh bỏ trống thì tạo câu trả lời rỗng để vẫn được chấm (0 điểm). */
    public Answer answerFor(ExamItem item) {
        return answers.computeIfAbsent(item.getOrder(), k -> new Answer(item, null));
    }

    /**
     * IN_PROGRESS -> SUBMITTED.
     * Trả về false nếu bài đã nộp từ trước: yêu cầu nộp lần hai bị bỏ qua,
     * giữ nguyên kết quả lần đầu (UC-16, luồng ngoại lệ 3a).
     */
    public boolean markSubmitted(EndReason reason, LocalDateTime now) {
        Objects.requireNonNull(reason);
        if (isLocked()) {
            return false;
        }
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = now;
        this.endReason = reason;
        return true;
    }

    /** SUBMITTED -> GRADED. Chỉ SubmissionGrader gọi, sau khi đã chấm từng câu. */
    public void markGraded(double total) {
        if (status != SubmissionStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ chấm được bài ở trạng thái SUBMITTED, hiện là " + status);
        }
        this.totalScore = total;
        this.status = SubmissionStatus.GRADED;
    }

    public boolean isLocked() {
        return status != SubmissionStatus.IN_PROGRESS;
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
