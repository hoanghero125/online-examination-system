package com.group2.oes.model;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.grading.GradingStrategy;
import com.group2.oes.model.question.Question;

import java.util.Objects;

/**
 * Một câu hỏi khi đã nằm trong một đề cụ thể.
 * Thứ tự và điểm thuộc về đề, không thuộc về câu hỏi gốc,
 * nên cùng một Question có thể mang điểm khác nhau ở hai đề khác nhau.
 */
public class ExamItem {

    private final int order;
    private final double point;
    private final Question question;

    public ExamItem(int order, double point, Question question) {
        ValidationException.require(order > 0, "ITEM_ORDER", "Thứ tự câu phải bắt đầu từ 1.");
        ValidationException.require(point > 0, "ITEM_POINT", "Điểm của câu hỏi trong đề phải lớn hơn 0.");
        this.order = order;
        this.point = point;
        this.question = Objects.requireNonNull(question);
    }

    public double grade(String response, GradingStrategy strategy) {
        return strategy.grade(question, response, point);
    }

    public int getOrder() {
        return order;
    }

    public double getPoint() {
        return point;
    }

    public Question getQuestion() {
        return question;
    }
}
