package com.group2.oes;

import com.group2.oes.client.ExamPaper;
import com.group2.oes.client.PaperItem;
import com.group2.oes.client.PaperOption;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.PaperShuffler;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static com.group2.oes.Fixtures.OOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperShufflerTest {

    private static final List<Integer> ONE_TO_TWENTY = IntStream.rangeClosed(1, 20).boxed().toList();

    @Test
    void cùngSeedCùngThứTự() {
        long seed = PaperShuffler.seedFor(5, 42);
        assertEquals(PaperShuffler.shuffle(ONE_TO_TWENTY, seed), PaperShuffler.shuffle(ONE_TO_TWENTY, seed));
    }

    @Test
    void khácThíSinhKhácThứTự() {
        long distinct = IntStream.range(1, 11)
                .mapToObj(student -> PaperShuffler.shuffle(ONE_TO_TWENTY, PaperShuffler.seedFor(5, student)))
                .distinct().count();
        assertEquals(10, distinct, "20 câu có 20! hoán vị, mười thí sinh không thể trùng nhau");
    }

    @Test
    void danhSáchGốcKhôngBịĐổi() {
        List<Integer> original = new java.util.ArrayList<>(ONE_TO_TWENTY);
        PaperShuffler.shuffle(original, 99);
        assertEquals(ONE_TO_TWENTY, original);
    }

    /**
     * Đề phía giảng viên (ExamItem, có đáp án) và đề phía thí sinh (PaperItem, không có đáp án)
     * phải ra cùng thứ tự câu và cùng thứ tự phương án, vì cả hai cùng dùng PaperShuffler với cùng seed.
     */
    @Test
    void haiPhíaThấyCùngThứTựCâuVàPhươngÁn() {
        Exam exam = Exam.draft("Đề", OOP, MultiChoicePolicy.PARTIAL);
        for (int i = 1; i <= 12; i++) {
            exam.addItem(i % 2 == 0 ? Fixtures.multi(i) : Fixtures.single(i, 1, Difficulty.EASY), 1);
        }
        LocalDateTime t = LocalDateTime.of(2026, 11, 20, 8, 0);
        SessionPolicy policy = new SessionPolicy(true, true, false);
        ExamSession session = new ExamSession(77, exam, new SessionWindow(t, t.plusHours(1)), policy);
        Student sv = new Student(9, "sv", "h$h", "SV", null, true, "B24009");

        ExamPaper paper = new ExamPaper(exam.getItems().stream().map(PaperShufflerTest::strip).toList())
                .shuffledFor(77, 9, policy);

        List<ExamItem> staffSide = session.buildPaperFor(sv);
        assertEquals(staffSide.stream().map(ExamItem::getOrder).toList(),
                paper.getItems().stream().map(PaperItem::order).toList());
        for (ExamItem item : staffSide) {
            assertEquals(session.optionsFor(sv, item.getQuestion()).stream().map(AnswerOption::getOptionId).toList(),
                    paper.get(item.getOrder()).options().stream().map(PaperOption::optionId).toList());
        }
    }

    @Test
    void khôngTrộnThìGiữThứTựGốc() {
        Exam exam = Exam.draft("Đề", OOP, MultiChoicePolicy.PARTIAL);
        for (int i = 1; i <= 5; i++) {
            exam.addItem(Fixtures.single(i, 1, Difficulty.EASY), 1);
        }
        ExamPaper paper = new ExamPaper(exam.getItems().stream().map(PaperShufflerTest::strip).toList())
                .shuffledFor(1, 1, new SessionPolicy(false, false, false));
        assertEquals(List.of(1, 2, 3, 4, 5), paper.getItems().stream().map(PaperItem::order).toList());
        assertTrue(paper.getItems().stream().allMatch(it -> it.options().get(0).optionId() == 11));
    }

    /** Bỏ đáp án, giống khung nhìn v_exam_paper. */
    private static PaperItem strip(ExamItem item) {
        Question q = item.getQuestion();
        return new PaperItem(item.getOrder(), item.getPoint(), q.getQuestionId(), q.getType(), q.getContent(),
                q.getOptions().stream().map(o -> new PaperOption(o.getOptionId(), o.getContent())).toList());
    }
}
