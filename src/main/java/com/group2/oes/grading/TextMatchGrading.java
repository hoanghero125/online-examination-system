package com.group2.oes.grading;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.Question;

/** Chấm câu điền khuyết: so khớp sau khi chuẩn hóa chuỗi. */
public class TextMatchGrading implements GradingStrategy {

    @Override
    public double grade(Question question, String response, double maxPoint) {
        if (!(question instanceof FillInBlankQuestion fill)) {
            throw new ValidationException("WRONG_STRATEGY",
                    "TextMatchGrading chỉ dùng cho câu điền khuyết, nhận được " + question.getType());
        }
        return fill.isCorrect(response) ? maxPoint : 0.0;
    }
}
