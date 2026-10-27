package com.group2.oes.client;

import com.group2.oes.model.EndReason;
import com.group2.oes.model.SubmissionStatus;

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
 * Các hàm đều synchronized: service ghi trên luồng nền trong lúc màn hình đọc trên luồng giao diện.
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

    public synchronized void recordAnswer(int order, String response) {
        if (response == null || response.isBlank()) {
            answers.remove(order);
        } else {
            answers.put(order, response);
        }
    }

    public synchronized void setFlag(int order, boolean value) {
        if (value) {
            flagged.add(order);
        } else {
            flagged.remove(order);
        }
    }

    public synchronized void setViolationCount(int count) {
        this.violationCount = count;
    }

    /** Cập nhật bản sao sau khi CSDL đã nhận lệnh nộp bài. */
    public synchronized void markSubmitted(EndReason reason) {
        this.status = SubmissionStatus.SUBMITTED;
        this.endReason = reason;
    }

    public synchronized boolean isLocked() {
        return status != SubmissionStatus.IN_PROGRESS;
    }

    public synchronized boolean isGraded() {
        return status == SubmissionStatus.GRADED;
    }

    public synchronized boolean isTerminatedByViolation() {
        return endReason == EndReason.VIOLATION;
    }

    public synchronized String getResponse(int order) {
        return answers.get(order);
    }

    public synchronized boolean isAnswered(int order) {
        return answers.containsKey(order);
    }

    public synchronized boolean isFlagged(int order) {
        return flagged.contains(order);
    }

    public synchronized int answeredCount() {
        return answers.size();
    }

    public synchronized int flaggedCount() {
        return flagged.size();
    }

    /** Bản chép tại thời điểm gọi (không phải bản xem trực tiếp), an toàn khi luồng khác đang ghi. */
    public synchronized Map<Integer, String> getAnswers() {
        return Map.copyOf(answers);
    }

    public synchronized Set<Integer> getFlagged() {
        return Set.copyOf(flagged);
    }

    public synchronized int getSubmissionId() {
        return submissionId;
    }

    public synchronized int getSessionId() {
        return sessionId;
    }

    public synchronized int getStudentId() {
        return studentId;
    }

    public synchronized SubmissionStatus getStatus() {
        return status;
    }

    public synchronized int getViolationCount() {
        return violationCount;
    }

    public synchronized EndReason getEndReason() {
        return endReason;
    }

    public synchronized Double getTotalScore() {
        return totalScore;
    }
}
