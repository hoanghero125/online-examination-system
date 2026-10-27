package com.group2.oes.client;

import com.group2.oes.model.EndReason;
import com.group2.oes.model.SubmissionStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Trạng thái bài làm của thí sinh trên máy trạm: câu đã trả lời, câu đánh dấu xem lại, số lần vi phạm, điểm.
 *
 * Bản này chỉ là bản sao để vẽ giao diện; CSDL mới là nơi quyết định. Mỗi lần ghi thành công xuống CSDL,
 * StudentExamService cập nhật lại bản sao này.
 */
public class AttemptState {

    private final int submissionId;
    private final int sessionId;
    private final int studentId;
    private SubmissionStatus status;
    private int violationCount;
    private EndReason endReason;
    private final Double totalScore;                             // null khi chưa chấm
    private final Map<Integer, String> answers = new HashMap<>(); // số câu gốc -> câu trả lời
    private final Set<Integer> flagged = new HashSet<>();

    public AttemptState(int submissionId, int sessionId, int studentId, SubmissionStatus status,
                        int violationCount, EndReason endReason, Double totalScore) {
        this.submissionId = submissionId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = Objects.requireNonNull(status);
        this.violationCount = violationCount;
        this.endReason = endReason;
        this.totalScore = totalScore;
    }

    /** Bài vừa tạo. */
    public static AttemptState started(int submissionId, int sessionId, int studentId) {
        return new AttemptState(submissionId, sessionId, studentId, SubmissionStatus.IN_PROGRESS, 0, null, null);
    }

    public void recordAnswer(int order, String response) {
        if (response == null || response.isBlank()) {
            answers.remove(order);
        } else {
            answers.put(order, response);
        }
    }

    public void setFlag(int order, boolean value) {
        if (value) {
            flagged.add(order);
        } else {
            flagged.remove(order);
        }
    }

    public void setViolationCount(int count) {
        this.violationCount = count;
    }

    /** Cập nhật bản sao sau khi CSDL đã nhận lệnh nộp bài. */
    public void markSubmitted(EndReason reason) {
        this.status = SubmissionStatus.SUBMITTED;
        this.endReason = reason;
    }

    public boolean isLocked() {
        return status != SubmissionStatus.IN_PROGRESS;
    }

    public boolean isGraded() {
        return status == SubmissionStatus.GRADED;
    }

    public boolean isTerminatedByViolation() {
        return endReason == EndReason.VIOLATION;
    }

    public String getResponse(int order) {
        return answers.get(order);
    }

    public boolean isAnswered(int order) {
        return answers.containsKey(order);
    }

    public boolean isFlagged(int order) {
        return flagged.contains(order);
    }

    public int answeredCount() {
        return answers.size();
    }

    public int flaggedCount() {
        return flagged.size();
    }

    public Map<Integer, String> getAnswers() {
        return Collections.unmodifiableMap(answers);
    }

    public Set<Integer> getFlagged() {
        return Collections.unmodifiableSet(flagged);
    }

    public int getSubmissionId() {
        return submissionId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getStudentId() {
        return studentId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public int getViolationCount() {
        return violationCount;
    }

    public EndReason getEndReason() {
        return endReason;
    }

    public Double getTotalScore() {
        return totalScore;
    }
}
