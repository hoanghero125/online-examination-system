package com.group2.oes.client;

import com.group2.oes.model.question.QuestionType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Một dòng của màn hình xem lại bài sau khi thi (FR-33).
 *
 * {@code response} và {@code correctAnswer} dùng cùng định dạng lưu trong CSDL
 * (mã phương án, "12,14", "true"/"false", văn bản). Riêng đáp án điền khuyết có thể có nhiều đáp án chấp nhận,
 * nối bằng " / ".
 *
 * @param earnedPoint   điểm của câu, null khi bài chưa chấm
 * @param correctAnswer đáp án đúng, null khi chưa được xem (ca chưa đóng hoặc ca không cho xem đáp án)
 */
public record ReviewItem(int order, double point, QuestionType type, String content, List<PaperOption> options,
                         String response, Double earnedPoint, String correctAnswer) {

    public ReviewItem {
        Objects.requireNonNull(type, "type");
        options = List.copyOf(options);
    }

    public boolean hasCorrectAnswer() {
        return correctAnswer != null;
    }

    public boolean isFullMark() {
        return earnedPoint != null && Math.abs(earnedPoint - point) < 1e-9;
    }

    /** Câu trả lời của thí sinh dạng người đọc được: đổi mã phương án thành nội dung, "true" thành "Đúng". */
    public String responseText() {
        return toText(response);
    }

    public String correctText() {
        return toText(correctAnswer);
    }

    private String toText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return switch (type) {
            case SINGLE_CHOICE, MULTI_CHOICE -> Arrays.stream(raw.split(","))
                    .map(String::strip)
                    .map(this::optionContent)
                    .collect(Collectors.joining("; "));
            case TRUE_FALSE -> "true".equalsIgnoreCase(raw.strip()) ? "Đúng" : "Sai";
            case FILL_IN_BLANK -> raw;
        };
    }

    private String optionContent(String id) {
        return options.stream()
                .filter(o -> String.valueOf(o.optionId()).equals(id))
                .map(PaperOption::content)
                .findFirst()
                .orElse("?");
    }
}
