package com.group2.oes;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.MultiChoiceQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.SingleChoiceQuestion;
import com.group2.oes.model.question.TrueFalseQuestion;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.List;

import static com.group2.oes.Fixtures.OOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionTest {

    @Test
    void singleChoiceChỉĐúngKhiChọnĐúngPhươngÁn() {
        Question q = Fixtures.single(1, 1, Difficulty.EASY);
        assertTrue(q.isCorrect("11"));
        assertTrue(q.isCorrect(" 11 "));
        assertFalse(q.isCorrect("12"));
        assertFalse(q.isCorrect(""));
        assertFalse(q.isCorrect(null));
        assertFalse(q.isCorrect("abc"), "dữ liệu hỏng không được làm sập chương trình");
    }

    @Test
    void multiChoiceTínhTỷLệCóTrừPhầnChọnSai() {
        MultiChoiceQuestion q = Fixtures.multi(2);
        assertEquals(1.0, q.correctRatio("21,22"));
        assertEquals(0.5, q.correctRatio("21"));
        assertEquals(0.0, q.correctRatio("21,23"), "1 đúng trừ 1 sai còn 0");
        assertEquals(0.0, q.correctRatio("21,22,23,24"), "chọn hết không được điểm");
        assertTrue(q.isCorrect("22, 21"), "thứ tự chọn không quan trọng");
        assertFalse(q.isCorrect("21"));
    }

    @Test
    void trueFalseKhôngNhậnChuỗiLạ() {
        Question q = Fixtures.trueFalse(3, false);
        assertTrue(q.isCorrect("false"));
        assertTrue(q.isCorrect("FALSE"));
        // Boolean.parseBoolean("có") trả về false, nếu không kiểm tra kỹ sẽ bị chấm là đúng
        assertFalse(q.isCorrect("có"));
    }

    @Test
    void điềnKhuyếtChuẩnHóaKhoảngTrắngHoaThườngVàDấuTiếngViệt() {
        FillInBlankQuestion q = Fixtures.fill(4, "kế thừa");
        assertTrue(q.isCorrect("Kế   Thừa "));
        String nfd = Normalizer.normalize("kế thừa", Normalizer.Form.NFD);
        assertFalse(nfd.equals("kế thừa"), "hai chuỗi trông giống nhau nhưng khác mã");
        assertTrue(q.isCorrect(nfd), "bộ gõ sinh dấu rời vẫn phải được chấm đúng");
        assertFalse(q.isCorrect("ke thua"));
    }

    @Test
    void câuMộtĐápÁnCóHaiPhươngÁnĐúngBịTừChối() {
        ValidationException e = assertThrows(ValidationException.class, () ->
                new SingleChoiceQuestion(0, OOP, "Lỗi", 1, Difficulty.EASY, 1,
                        List.of(AnswerOption.of("A", true), AnswerOption.of("B", true))));
        assertEquals("SINGLE_NEEDS_ONE_CORRECT", e.getCode());
    }

    @Test
    void dữLiệuChungKhôngHợpLệBịChặnỞLớpCha() {
        // kiểm tra nằm trong constructor của Question nên mọi lớp con đều được hưởng
        assertEquals("POINT_INVALID", assertThrows(ValidationException.class,
                () -> new TrueFalseQuestion(0, OOP, "x", 1, Difficulty.EASY, 0.0, true)).getCode());
        assertEquals("QUESTION_EMPTY", assertThrows(ValidationException.class,
                () -> new TrueFalseQuestion(0, OOP, "   ", 1, Difficulty.EASY, 1, true)).getCode());
    }
}
