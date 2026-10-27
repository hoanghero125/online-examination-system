package com.group2.oes.grading;

import com.group2.oes.model.question.Question;

/** Đúng hoàn toàn thì trọn điểm, ngược lại 0 điểm. */
public class ExactMatchGrading implements GradingStrategy {

    @Override
    public double grade(Question question, String response, double maxPoint) {
        return question.isCorrect(response) ? maxPoint : 0.0;
    }
}
