package com.group2.oes.factory;

import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.AnswerOption;

import java.util.List;

/**
 * Gói dữ liệu thô để dựng một câu hỏi, chưa biết thuộc lớp con nào.
 * Dữ liệu đến từ biểu mẫu nhập liệu, từ tệp CSV hoặc từ một dòng trong CSDL.
 * Các trường không dùng tới với loại câu hỏi đang tạo thì để null.
 */
public record QuestionData(
        int questionId,
        Subject subject,
        String content,
        int chapter,
        Difficulty difficulty,
        double defaultPoint,
        List<AnswerOption> options,     // một đáp án, nhiều đáp án
        Boolean correctValue,           // đúng/sai
        List<String> acceptedAnswers,   // điền khuyết
        boolean caseSensitive           // điền khuyết
) {

    /** Dữ liệu chung, chưa có phần riêng của lớp con. Dùng kèm các hàm with... bên dưới. */
    public static QuestionData basic(Subject subject, String content, int chapter,
                                     Difficulty difficulty, double point) {
        return new QuestionData(0, subject, content, chapter, difficulty, point,
                null, null, null, false);
    }

    public QuestionData withOptions(List<AnswerOption> opts) {
        return new QuestionData(questionId, subject, content, chapter, difficulty, defaultPoint,
                opts, correctValue, acceptedAnswers, caseSensitive);
    }

    public QuestionData withCorrectValue(boolean value) {
        return new QuestionData(questionId, subject, content, chapter, difficulty, defaultPoint,
                options, value, acceptedAnswers, caseSensitive);
    }

    public QuestionData withAccepted(List<String> answers, boolean caseSens) {
        return new QuestionData(questionId, subject, content, chapter, difficulty, defaultPoint,
                options, correctValue, answers, caseSens);
    }

    public QuestionData withId(int id) {
        return new QuestionData(id, subject, content, chapter, difficulty, defaultPoint,
                options, correctValue, acceptedAnswers, caseSensitive);
    }
}
