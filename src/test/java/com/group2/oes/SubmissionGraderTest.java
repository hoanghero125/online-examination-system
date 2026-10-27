package com.group2.oes;

import com.group2.oes.grading.SubmissionGrader;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.SingleChoiceQuestion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.group2.oes.Fixtures.OOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionGraderTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);
    private static final Student SV = new Student(7, "sv", "h$h", "SV", null, true, "B24007");

    private final SubmissionGrader grader = new SubmissionGrader();

    private static Submission submittedFor(Exam exam) {
        ExamSession session = new ExamSession(1, exam, new SessionWindow(T0, T0.plusHours(1)),
                new SessionPolicy(false, false, false));
        return new Submission(SV, session, T0);
    }

    /** Câu hỏi "hỏng": chấm tới là ném lỗi, giả lập dữ liệu lỗi trong CSDL. */
    private static SingleChoiceQuestion broken(int id) {
        return new SingleChoiceQuestion(id, OOP, "Câu hỏng", 1, Difficulty.EASY, 1,
                List.of(new AnswerOption(11, "A", true), new AnswerOption(12, "B", false))) {
            @Override
            public boolean isCorrect(String response) {
                throw new IllegalStateException("dữ liệu hỏng");
            }
        };
    }

    @Test
    void mộtCâuNémLỗiThìCácCâuCònLạiVẫnĐượcChấm() {
        Exam exam = Exam.draft("Đề", OOP, MultiChoicePolicy.PARTIAL);
        exam.addItem(Fixtures.single(1, 1, Difficulty.EASY), 2);
        exam.addItem(broken(2), 3);
        exam.addItem(Fixtures.trueFalse(3, true), 5);
        Submission s = submittedFor(exam);
        s.restoreAnswer(1, "11");
        s.restoreAnswer(2, "11");
        s.restoreAnswer(3, "true");
        s.markSubmitted(EndReason.MANUAL, T0.plusMinutes(30));

        assertEquals(7.0, grader.grade(s));
        assertEquals(SubmissionStatus.GRADED, s.getStatus());
        assertTrue(s.getAnswer(2).hasGradingError(), "câu lỗi được đánh dấu để giảng viên rà lại");
        assertEquals(0.0, s.getAnswer(2).getEarnedPoint());
        assertFalse(s.getAnswer(3).hasGradingError());
    }

    @Test
    void câuBỏTrốngĐược0ĐiểmVàVẫnCóCâuTrảLờiĐểThốngKê() {
        Exam exam = Exam.draft("Đề", OOP, MultiChoicePolicy.ALL_OR_NOTHING);
        exam.addItem(Fixtures.single(1, 1, Difficulty.EASY), 1);
        exam.addItem(Fixtures.multi(2), 1);
        Submission s = submittedFor(exam);
        s.restoreAnswer(1, "11");
        s.markSubmitted(EndReason.TIME_OUT, T0.plusHours(1));

        assertEquals(1.0, grader.grade(s));
        assertEquals(0.0, s.getAnswer(2).getEarnedPoint());
        assertTrue(s.getAnswer(2).isGraded());
    }

    @Test
    void chiếnLượcChấmCâuNhiềuĐápÁnTheoCấuHìnhĐề() {
        Exam partial = Exam.draft("Tỷ lệ", OOP, MultiChoicePolicy.PARTIAL);
        partial.addItem(Fixtures.multi(1), 4);
        Submission s = submittedFor(partial);
        s.restoreAnswer(1, "21");   // đúng 1 trong 2 phương án đúng
        s.markSubmitted(EndReason.MANUAL, T0);
        assertEquals(2.0, grader.grade(s));
    }

    @Test
    void chỉChấmĐượcBàiĐãNộp() {
        Exam exam = Exam.draft("Đề", OOP, MultiChoicePolicy.PARTIAL);
        exam.addItem(Fixtures.trueFalse(1, true), 1);
        Submission s = submittedFor(exam);
        assertThrows(IllegalStateException.class, () -> grader.grade(s));
    }

    @Test
    void nộpHaiLầnThìLầnSauBịBỏQua() {
        Exam exam = Exam.draft("Đề", OOP, MultiChoicePolicy.PARTIAL);
        exam.addItem(Fixtures.trueFalse(1, true), 1);
        Submission s = submittedFor(exam);
        assertTrue(s.markSubmitted(EndReason.MANUAL, T0));
        assertFalse(s.markSubmitted(EndReason.TIME_OUT, T0.plusMinutes(1)));
        assertEquals(EndReason.MANUAL, s.getEndReason());
    }

    /** NFR-02: chấm một bài 50 câu dưới 3 giây. */
    @Test
    void chấmBàiNămMươiCâuDướiBaGiây() {
        Exam exam = Exam.draft("50 câu", OOP, MultiChoicePolicy.PARTIAL);
        for (int i = 1; i <= 50; i++) {
            switch (i % 4) {
                case 0 -> exam.addItem(Fixtures.single(i, 1, Difficulty.EASY), 1);
                case 1 -> exam.addItem(Fixtures.multi(i), 1);
                case 2 -> exam.addItem(Fixtures.trueFalse(i, true), 1);
                default -> exam.addItem(Fixtures.fill(i, "đóng gói", "encapsulation"), 1);
            }
        }
        Submission s = submittedFor(exam);
        for (int i = 1; i <= 50; i++) {
            s.restoreAnswer(i, switch (i % 4) {
                case 0 -> "11";
                case 1 -> "21,22";
                case 2 -> "true";
                default -> "Đóng   Gói";
            });
        }
        s.markSubmitted(EndReason.MANUAL, T0);
        double total = assertTimeout(Duration.ofSeconds(3), () -> grader.grade(s));
        assertEquals(50.0, total);
    }
}
