package com.group2.oes.model.question;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class FillInBlankQuestion extends Question {

    private final List<String> accepted;
    private final boolean caseSensitive;

    public FillInBlankQuestion(int questionId, Subject subject, String content, int chapter,
                               Difficulty difficulty, double defaultPoint,
                               List<String> accepted, boolean caseSensitive) {
        super(questionId, subject, content, chapter, difficulty, defaultPoint);
        ValidationException.require(accepted != null && accepted.stream().anyMatch(s -> s != null && !s.isBlank()),
                "FILL_NEEDS_ANSWER", "Câu điền khuyết cần ít nhất 1 đáp án chấp nhận.");
        this.caseSensitive = caseSensitive;
        // chuẩn hóa sẵn một lần lúc tạo, lúc chấm chỉ còn phải chuẩn hóa câu trả lời
        this.accepted = accepted.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(a -> normalize(a, caseSensitive))
                .distinct()
                .toList();
    }

    @Override
    public QuestionType getType() {
        return QuestionType.FILL_IN_BLANK;
    }

    public List<String> getAccepted() {
        return accepted;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Đưa chuỗi về dạng so sánh được:
     * - NFC: bộ gõ tiếng Việt có thể sinh "ế" dưới dạng một ký tự hoặc "e" + dấu rời,
     *   hai cách nhìn giống hệt nhau nhưng equals trả false nếu không chuẩn hóa.
     * - gộp khoảng trắng thừa, bỏ khoảng trắng hai đầu
     * - đưa về chữ thường nếu câu hỏi không phân biệt hoa thường
     */
    public String normalize(String s) {
        return normalize(s, caseSensitive);
    }

    private static String normalize(String s, boolean caseSensitive) {
        String r = Normalizer.normalize(s, Normalizer.Form.NFC).strip().replaceAll("\\s+", " ");
        return caseSensitive ? r : r.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean isCorrect(String response) {
        return !isBlank(response) && accepted.contains(normalize(response));
    }

    @Override
    public double correctRatio(String response) {
        return isCorrect(response) ? 1.0 : 0.0;
    }
}
