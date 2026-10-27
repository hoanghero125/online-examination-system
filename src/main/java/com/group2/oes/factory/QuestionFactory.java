package com.group2.oes.factory;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.MultiChoiceQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.model.question.SingleChoiceQuestion;
import com.group2.oes.model.question.TrueFalseQuestion;

import java.util.Objects;

/**
 * Nơi duy nhất trong chương trình biết loại câu hỏi nào ứng với lớp con nào.
 *
 * Được gọi từ hai chỗ: tầng service khi giảng viên lưu câu hỏi mới,
 * và tầng DAO khi đọc một dòng của bảng question lên.
 * Thêm loại câu hỏi mới chỉ cần thêm một lớp con và một nhánh ở đây.
 */
public final class QuestionFactory {

    private QuestionFactory() {
    }

    public static Question create(QuestionType type, QuestionData d) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(d, "data");

        // switch trên enum: thiếu một nhánh thì trình biên dịch báo lỗi, không bị bỏ sót
        return switch (type) {
            case SINGLE_CHOICE -> new SingleChoiceQuestion(d.questionId(), d.subject(), d.content(),
                    d.chapter(), d.difficulty(), d.defaultPoint(), d.options());

            case MULTI_CHOICE -> new MultiChoiceQuestion(d.questionId(), d.subject(), d.content(),
                    d.chapter(), d.difficulty(), d.defaultPoint(), d.options());

            case TRUE_FALSE -> {
                ValidationException.require(d.correctValue() != null,
                        "TF_NEEDS_VALUE", "Câu đúng/sai chưa chọn đáp án đúng.");
                yield new TrueFalseQuestion(d.questionId(), d.subject(), d.content(),
                        d.chapter(), d.difficulty(), d.defaultPoint(), d.correctValue());
            }

            case FILL_IN_BLANK -> new FillInBlankQuestion(d.questionId(), d.subject(), d.content(),
                    d.chapter(), d.difficulty(), d.defaultPoint(),
                    d.acceptedAnswers(), d.caseSensitive());
        };
    }
}
