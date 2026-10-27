package com.group2.oes.model.question;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;

import java.util.List;

public class SingleChoiceQuestion extends Question {

    private final List<AnswerOption> options;

    public SingleChoiceQuestion(int questionId, Subject subject, String content, int chapter,
                                Difficulty difficulty, double defaultPoint, List<AnswerOption> options) {
        super(questionId, subject, content, chapter, difficulty, defaultPoint);
        ValidationException.require(options != null && options.size() >= 2,
                "OPTIONS_TOO_FEW", "Câu trắc nghiệm cần ít nhất 2 phương án.");
        long correct = options.stream().filter(AnswerOption::isCorrect).count();
        ValidationException.require(correct == 1,
                "SINGLE_NEEDS_ONE_CORRECT", "Câu một đáp án phải có đúng 1 phương án đúng, hiện có " + correct + ".");
        this.options = List.copyOf(options);
    }

    @Override
    public QuestionType getType() {
        return QuestionType.SINGLE_CHOICE;
    }

    @Override
    public List<AnswerOption> getOptions() {
        return options;
    }

    @Override
    public boolean isCorrect(String response) {
        if (isBlank(response)) {
            return false;
        }
        try {
            int chosen = Integer.parseInt(response.strip());
            return options.stream().anyMatch(o -> o.isCorrect() && o.getOptionId() == chosen);
        } catch (NumberFormatException e) {
            // dữ liệu hỏng thì coi như trả lời sai, không làm sập cả lượt chấm
            return false;
        }
    }

    @Override
    public double correctRatio(String response) {
        return isCorrect(response) ? 1.0 : 0.0;
    }
}
