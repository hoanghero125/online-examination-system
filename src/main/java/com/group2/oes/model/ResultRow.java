package com.group2.oes.model;

/**
 * Một dòng của bảng kết quả ca thi phía giảng viên (FR-30, FR-34).
 * Bài chưa chấm có điểm null; cột "kết thúc do vi phạm" được tô màu trên màn hình (BR-06).
 */
public record ResultRow(int submissionId, String studentCode, String fullName, SubmissionStatus status,
                        Double score, Double scoreOn10, long answeredCount, int violationCount, EndReason endReason) {

    public static ResultRow of(Submission s) {
        boolean graded = s.getStatus() == SubmissionStatus.GRADED;
        return new ResultRow(s.getSubmissionId(), s.getStudent().getStudentCode(), s.getStudent().getFullName(),
                s.getStatus(), graded ? s.getTotalScore() : null, graded ? s.scoreOn10() : null,
                s.answeredCount(), s.getViolationCount(), s.getEndReason());
    }

    public boolean terminatedByViolation() {
        return endReason == EndReason.VIOLATION;
    }
}
