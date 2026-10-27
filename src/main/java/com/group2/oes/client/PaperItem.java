package com.group2.oes.client;

import com.group2.oes.model.question.QuestionType;

import java.util.List;
import java.util.Objects;

/**
 * Một câu trong đề như máy thí sinh nhìn thấy: nội dung, điểm, phương án, không có đáp án.
 *
 * {@code order} là số thứ tự câu trong đề GỐC. Câu trả lời luôn lưu theo số này,
 * nên thứ tự hiển thị sau khi trộn không ảnh hưởng tới việc chấm.
 * Câu đúng/sai và điền khuyết không có phương án (danh sách rỗng).
 */
public record PaperItem(int order, double point, int questionId, QuestionType type, String content,
                        List<PaperOption> options) {

    public PaperItem {
        Objects.requireNonNull(type, "type");
        options = List.copyOf(options);
    }

    public PaperItem withOptions(List<PaperOption> reordered) {
        return new PaperItem(order, point, questionId, type, content, reordered);
    }
}
