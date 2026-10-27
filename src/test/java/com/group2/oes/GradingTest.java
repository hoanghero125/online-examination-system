package com.group2.oes;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.factory.QuestionFactory;
import com.group2.oes.grading.ExactMatchGrading;
import com.group2.oes.grading.GradingStrategy;
import com.group2.oes.grading.PartialMatchGrading;
import com.group2.oes.grading.TextMatchGrading;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Exam;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.MultiChoiceQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.model.question.TrueFalseQuestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.group2.oes.Fixtures.OOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GradingTest {

    private final MultiChoiceQuestion multi = Fixtures.multi(1);

    @ParameterizedTest(name = "chọn \"{0}\": toàn phần {1}, theo tỷ lệ {2}")
    @CsvSource({
            "'21,22',   4.0, 4.0",
            "'21',      0.0, 2.0",
            "'21,23',   0.0, 0.0",
            "'',        0.0, 0.0",
    })
    void cùngMộtCâuHaiChiếnLượcChoHaiKếtQuả(String response, double exact, double partial) {
        GradingStrategy all = new ExactMatchGrading();
        GradingStrategy part = new PartialMatchGrading();
        assertEquals(exact, all.grade(multi, response, 4.0));
        assertEquals(partial, part.grade(multi, response, 4.0));
    }

    @Test
    void điểmTheoTỷLệĐượcLàmTrònHaiChữSố() {
        MultiChoiceQuestion three = new MultiChoiceQuestion(2, OOP, "3 đáp án đúng", 1, Difficulty.EASY, 1,
                List.of(new AnswerOption(1, "A", true), new AnswerOption(2, "B", true),
                        new AnswerOption(3, "C", true), new AnswerOption(4, "D", false)));
        assertEquals(0.67, new PartialMatchGrading().grade(three, "1,2", 1.0));
    }

    @Test
    void textMatchTừChốiCâuKhôngPhảiĐiềnKhuyết() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> new TextMatchGrading().grade(multi, "21", 1));
        assertEquals("WRONG_STRATEGY", e.getCode());
    }

    @Test
    void đềChọnChiếnLượcTheoLoạiCâuVàCấuHình() {
        Exam partial = Exam.draft("A", OOP, MultiChoicePolicy.PARTIAL);
        Exam strict = Exam.draft("B", OOP, MultiChoicePolicy.ALL_OR_NOTHING);
        assertInstanceOf(PartialMatchGrading.class, partial.strategyFor(multi));
        assertInstanceOf(ExactMatchGrading.class, strict.strategyFor(multi));
        assertInstanceOf(TextMatchGrading.class, partial.strategyFor(Fixtures.fill(9, "x")));
        // chiến lược không giữ trạng thái nên mọi đề dùng chung một thể hiện
        assertSame(partial.strategyFor(Fixtures.trueFalse(1, true)), strict.strategyFor(Fixtures.trueFalse(2, true)));
    }

    @Test
    void factoryDựngĐúngLớpConTheoLoại() {
        QuestionData base = QuestionData.basic(OOP, "Nội dung", 1, Difficulty.EASY, 1);
        Question tf = QuestionFactory.create(QuestionType.TRUE_FALSE, base.withCorrectValue(true));
        Question fill = QuestionFactory.create(QuestionType.FILL_IN_BLANK, base.withAccepted(List.of("abc"), false));
        assertInstanceOf(TrueFalseQuestion.class, tf);
        assertInstanceOf(FillInBlankQuestion.class, fill);
        assertEquals("TF_NEEDS_VALUE", assertThrows(ValidationException.class,
                () -> QuestionFactory.create(QuestionType.TRUE_FALSE, base)).getCode());
    }
}
