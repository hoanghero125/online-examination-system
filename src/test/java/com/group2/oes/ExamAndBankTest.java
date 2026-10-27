package com.group2.oes;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.TrueFalseQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static com.group2.oes.Fixtures.OOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamAndBankTest {

    private QuestionBank bank;

    @BeforeEach
    void setUp() {
        bank = new QuestionBank(OOP);
        for (int i = 1; i <= 5; i++) {
            bank.add(Fixtures.single(i, 1, Difficulty.EASY));
        }
        for (int i = 6; i <= 8; i++) {
            bank.add(Fixtures.single(i, 2, Difficulty.HARD));
        }
    }

    @Test
    void sinhĐềĐúngSốCâuTheoMaTrận() {
        ExamMatrix m = new ExamMatrix().add(1, Difficulty.EASY, 3).add(2, Difficulty.HARD, 2);
        List<Question> picked = bank.pick(m, new Random(42));
        assertEquals(5, picked.size());
        assertEquals(3, picked.stream().filter(q -> q.getChapter() == 1).count());
        assertEquals(picked.size(), picked.stream().distinct().count(), "không lấy trùng câu");
    }

    @Test
    void cùngSeedThìCùngBộĐề() {
        ExamMatrix m = new ExamMatrix().add(1, Difficulty.EASY, 3);
        assertEquals(bank.pick(m, new Random(7)), bank.pick(m, new Random(7)));
    }

    @Test
    void thiếuCâuThìBáoRõThiếuỞĐâu() {
        ExamMatrix m = new ExamMatrix().add(2, Difficulty.HARD, 10).add(3, Difficulty.EASY, 1);
        ValidationException e = assertThrows(ValidationException.class, () -> bank.pick(m, new Random()));
        assertTrue(e.getMessage().contains("chương 2 mức HARD: cần 10, có 3"));
        assertTrue(e.getMessage().contains("chương 3 mức EASY: cần 1, có 0"));
    }

    @Test
    void maTrậnKhôngChoKhaiBáoTrùngDòng() {
        assertThrows(ValidationException.class,
                () -> new ExamMatrix().add(1, Difficulty.EASY, 2).add(1, Difficulty.EASY, 1));
    }

    @Test
    void đềKhôngNhậnCâuTrùngHoặcKhácMôn() {
        Exam exam = Exam.draft("Giữa kỳ", OOP, MultiChoicePolicy.PARTIAL);
        Question q = bank.get(1);
        exam.addItem(q, 1);
        assertEquals("DUPLICATE_QUESTION", assertThrows(ValidationException.class, () -> exam.addItem(q, 2)).getCode());

        Subject db = Fixtures.DB;
        Question other = new TrueFalseQuestion(99, db, "Khác môn", 1, Difficulty.EASY, 1, true);
        assertEquals("WRONG_SUBJECT", assertThrows(ValidationException.class, () -> exam.addItem(other, 1)).getCode());
    }

    @Test
    void đềĐãKhóaPhảiTạoPhiênBảnMới() {
        Exam exam = Exam.draft("Cuối kỳ", OOP, MultiChoicePolicy.PARTIAL);
        exam.addItem(bank.get(1), 2);
        exam.addItem(bank.get(2), 3);
        exam.lock();
        assertEquals("EXAM_LOCKED", assertThrows(ValidationException.class,
                () -> exam.addItem(bank.get(3), 1)).getCode());

        Exam v2 = exam.newVersion();
        v2.addItem(bank.get(3), 1);
        assertEquals(2, v2.getVersion());
        assertEquals(6.0, v2.getTotalPoint());
        assertEquals(5.0, exam.getTotalPoint(), "bản cũ giữ nguyên");
    }

    @Test
    void đềRỗngKhôngKhóaĐược() {
        Exam empty = Exam.draft("Rỗng", OOP, MultiChoicePolicy.PARTIAL);
        assertEquals("EXAM_EMPTY", assertThrows(ValidationException.class, empty::lock).getCode());
    }
}
