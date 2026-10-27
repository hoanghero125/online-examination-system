package com.group2.oes.model.question;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MultiChoiceQuestion extends Question {

    private final List<AnswerOption> options;

    public MultiChoiceQuestion(int questionId, Subject subject, String content, int chapter,
                               Difficulty difficulty, double defaultPoint, List<AnswerOption> options) {
        super(questionId, subject, content, chapter, difficulty, defaultPoint);
        ValidationException.require(options != null && options.size() >= 2,
                "OPTIONS_TOO_FEW", "Câu trắc nghiệm cần ít nhất 2 phương án.");
        ValidationException.require(options.stream().anyMatch(AnswerOption::isCorrect),
                "MULTI_NEEDS_CORRECT", "Câu nhiều đáp án phải có ít nhất 1 phương án đúng.");
        this.options = List.copyOf(options);
    }

    @Override
    public QuestionType getType() {
        return QuestionType.MULTI_CHOICE;
    }

    @Override
    public List<AnswerOption> getOptions() {
        return options;
    }

    @Override
    public boolean isCorrect(String response) {
        return parse(response).equals(correctIds());
    }

    /**
     * Tỷ lệ đúng = (số phương án đúng đã chọn - số phương án sai đã chọn) / tổng số phương án đúng,
     * chặn dưới ở 0. Trừ điểm phần chọn sai để thí sinh không thể chọn hết mọi phương án mà vẫn có điểm.
     */
    @Override
    public double correctRatio(String response) {
        Set<Integer> chosen = parse(response);
        Set<Integer> correct = correctIds();

        Set<Integer> hit = new HashSet<>(chosen);
        hit.retainAll(correct);              // giao: chọn đúng
        Set<Integer> wrong = new HashSet<>(chosen);
        wrong.removeAll(correct);            // hiệu: chọn sai

        double ratio = (double) (hit.size() - wrong.size()) / correct.size();
        return Math.max(0.0, ratio);
    }

    private Set<Integer> correctIds() {
        return options.stream()
                .filter(AnswerOption::isCorrect)
                .map(AnswerOption::getOptionId)
                .collect(Collectors.toSet());
    }

    private static Set<Integer> parse(String response) {
        if (isBlank(response)) {
            return Set.of();
        }
        try {
            return Arrays.stream(response.split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            return Set.of();
        }
    }
}
