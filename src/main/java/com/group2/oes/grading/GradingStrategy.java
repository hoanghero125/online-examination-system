package com.group2.oes.grading;

import com.group2.oes.model.question.Question;

/**
 * Cách tính điểm cho một câu trả lời.
 * Lớp Answer chỉ gọi grade(...) và không cần biết bên trong tính thế nào.
 */
public interface GradingStrategy {

    /**
     * @param maxPoint điểm tối đa của câu này trong đề (lấy từ ExamItem, không phải điểm mặc định)
     * @return điểm đạt được, luôn nằm trong [0, maxPoint]
     */
    double grade(Question question, String response, double maxPoint);
}
