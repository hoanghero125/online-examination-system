package com.group2.oes.model.question;

import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;

import java.util.List;

public class TrueFalseQuestion extends Question {

    private final boolean correctValue;

    public TrueFalseQuestion(int questionId, Subject subject, String content, int chapter,
                             Difficulty difficulty, double defaultPoint, boolean correctValue) {
        super(questionId, subject, content, chapter, difficulty, defaultPoint);
        this.correctValue = correctValue;
    }

    @Override
    public QuestionType getType() {
        return QuestionType.TRUE_FALSE;
    }

    public boolean getCorrectValue() {
        return correctValue;
    }

    @Override
    public List<String> getDisplayOptions() {
        return List.of("Đúng", "Sai");
    }

    @Override
    public boolean isCorrect(String response) {
        if (isBlank(response)) {
            return false;
        }
        String r = response.strip();
        // Boolean.parseBoolean trả false cho mọi chuỗi lạ, nên phải kiểm tra rõ ràng
        if (!r.equalsIgnoreCase("true") && !r.equalsIgnoreCase("false")) {
            return false;
        }
        return Boolean.parseBoolean(r) == correctValue;
    }

    @Override
    public double correctRatio(String response) {
        return isCorrect(response) ? 1.0 : 0.0;
    }
}
