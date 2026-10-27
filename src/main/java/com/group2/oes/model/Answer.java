package com.group2.oes.model;

import com.group2.oes.grading.GradingStrategy;

import java.util.Objects;

/** Câu trả lời của thí sinh cho một mục trong đề. */
public class Answer {

    private final ExamItem examItem;
    private String response;
    private double earnedPoint;
    private boolean graded;
    private boolean gradingError;

    public Answer(ExamItem examItem, String response) {
        this.examItem = Objects.requireNonNull(examItem);
        this.response = response;
    }

    public void grade(GradingStrategy strategy) {
        this.earnedPoint = examItem.grade(response, strategy);
        this.graded = true;
        this.gradingError = false;
    }

    /** Câu này chấm lỗi: cho 0 điểm tạm thời và đánh dấu để giảng viên rà lại. */
    void markGradingError() {
        this.earnedPoint = 0;
        this.graded = false;
        this.gradingError = true;
    }

    /** Dùng khi DAO dựng lại bài làm đã chấm từ CSDL. */
    public void restoreResult(double point) {
        this.earnedPoint = point;
        this.graded = true;
    }

    void setResponse(String response) {
        this.response = response;
    }

    public boolean isAnswered() {
        return response != null && !response.isBlank();
    }

    /** Được trọn điểm của câu. */
    public boolean isFullMark() {
        return graded && Math.abs(earnedPoint - examItem.getPoint()) < 1e-9;
    }

    public ExamItem getExamItem() {
        return examItem;
    }

    public String getResponse() {
        return response;
    }

    public double getEarnedPoint() {
        return earnedPoint;
    }

    public boolean isGraded() {
        return graded;
    }

    public boolean hasGradingError() {
        return gradingError;
    }
}
