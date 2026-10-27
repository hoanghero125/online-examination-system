package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.dao.memory.ManualClock;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.ProctoringService.ViolationResult;
import com.group2.oes.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm thử các luồng nghiệp vụ chính qua tầng service, dùng DAO bộ nhớ và đồng hồ giả lập. */
class SubmissionFlowTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);

    private ManualClock clock;
    private AppContext app;
    private Instructor teacher;
    private Student sv1;
    private Student sv2;
    private Student outsider;
    private Exam exam;
    private ExamSession session;
    private int sid;

    @BeforeEach
    void setUp() {
        clock = new ManualClock(T0.minusDays(1));
        app = AppContext.inMemory(clock);

        Administrator admin = app.users.insert(new Administrator(0, "admin", PasswordHasher.hash("a"), "QTV", null, true));
        Subject oop = app.subjects.insert("INT1332", "OOP");
        teacher = app.auth.createUser(admin, new Instructor(0, "gv", PasswordHasher.hash("g"), "GV", null, true));
        app.subjects.assignInstructor(teacher, oop);
        sv1 = app.auth.createUser(admin, new Student(0, "sv1", PasswordHasher.hash("s"), "SV 1", null, true, "B24001"));
        sv2 = app.auth.createUser(admin, new Student(0, "sv2", PasswordHasher.hash("s"), "SV 2", null, true, "B24002"));
        outsider = app.auth.createUser(admin, new Student(0, "sv3", PasswordHasher.hash("s"), "SV 3", null, true, "B24003"));

        QuestionData base = QuestionData.basic(oop, "Câu", 1, Difficulty.EASY, 1);
        Question q1 = app.bankService.addQuestion(teacher, QuestionType.SINGLE_CHOICE,
                base.withOptions(List.of(AnswerOption.of("Đúng", true), AnswerOption.of("Sai", false))));
        Question q2 = app.bankService.addQuestion(teacher, QuestionType.TRUE_FALSE, base.withCorrectValue(true));
        Question q3 = app.bankService.addQuestion(teacher, QuestionType.FILL_IN_BLANK,
                base.withAccepted(List.of("đa hình"), false));
        QuestionBank bank = app.bankService.loadBank(teacher, oop);

        Map<Integer, Double> picks = new LinkedHashMap<>();
        picks.put(q1.getQuestionId(), 4.0);
        picks.put(q2.getQuestionId(), 3.0);
        picks.put(q3.getQuestionId(), 3.0);
        exam = app.examService.createManual(teacher, bank, "Giữa kỳ", MultiChoicePolicy.PARTIAL, picks);

        session = app.sessionService.createSession(teacher, exam, T0, T0.plusMinutes(60),
                true, true, true, List.of(sv1, sv2));
        sid = session.getSessionId();
        clock.set(T0.plusMinutes(5));   // ca thi đang mở
    }

    private String correctOf(int order) {
        Question q = exam.getItem(order).getQuestion();
        return switch (q.getType()) {
            case SINGLE_CHOICE -> String.valueOf(q.getOptions().stream().filter(AnswerOption::isCorrect)
                    .findFirst().orElseThrow().getOptionId());
            case TRUE_FALSE -> "true";
            case FILL_IN_BLANK -> "Đa Hình";
            default -> throw new IllegalStateException();
        };
    }

    @Test
    void làmĐúngHếtĐượcTrọnĐiểm() {
        app.submissionService.start(sv1, sid);
        for (int i = 1; i <= 3; i++) {
            app.submissionService.saveAnswer(sv1, sid, i, correctOf(i));
        }
        Submission s = app.submissionService.submit(sv1, sid, EndReason.MANUAL);
        assertEquals(SubmissionStatus.GRADED, s.getStatus());
        assertEquals(10.0, s.getTotalScore());
    }

    @Test
    void mỗiThíSinhChỉThiMộtLần() {
        app.submissionService.start(sv1, sid);
        app.submissionService.submit(sv1, sid, EndReason.MANUAL);
        SessionRuleException e = assertThrows(SessionRuleException.class, () -> app.submissionService.start(sv1, sid));
        assertEquals(SessionRuleException.ALREADY_SUBMITTED, e.getCode());
    }

    @Test
    void thoátRaVàoLạiThìMởĐúngBàiCũ() {
        Submission first = app.submissionService.start(sv1, sid);
        app.submissionService.saveAnswer(sv1, sid, 2, "true");
        app.proctoring.report(sv1, sid, Instant.EPOCH);

        Submission again = app.submissionService.start(sv1, sid);
        assertEquals(first.getSubmissionId(), again.getSubmissionId());
        assertEquals("true", again.getAnswer(2).getResponse(), "câu trả lời cũ còn nguyên");
        assertEquals(1, again.getViolationCount(), "số lần vi phạm không bị xóa khi vào lại");
    }

    @Test
    void nộpHaiLầnGiữKếtQuảLầnĐầu() {
        app.submissionService.start(sv1, sid);
        app.submissionService.saveAnswer(sv1, sid, 1, correctOf(1));
        Submission a = app.submissionService.submit(sv1, sid, EndReason.MANUAL);
        Submission b = app.submissionService.submit(sv1, sid, EndReason.TIME_OUT);
        assertEquals(a.getTotalScore(), b.getTotalScore());
        assertEquals(EndReason.MANUAL, b.getEndReason());
    }

    @Test
    void ngoàiGiờHoặcKhôngCóTênThìKhôngVàoĐược() {
        clock.set(T0.minusMinutes(1));
        assertEquals(SessionRuleException.NOT_OPEN_YET,
                assertThrows(SessionRuleException.class, () -> app.submissionService.start(sv1, sid)).getCode());
        clock.set(T0.plusMinutes(60));
        assertEquals(SessionRuleException.CLOSED,
                assertThrows(SessionRuleException.class, () -> app.submissionService.start(sv1, sid)).getCode());
        clock.set(T0.plusMinutes(5));
        assertEquals(SessionRuleException.NOT_ENROLLED,
                assertThrows(SessionRuleException.class, () -> app.submissionService.start(outsider, sid)).getCode());
    }

    @Test
    void hếtGiờThìKhôngGhiĐượcVàBàiTựNộp() {
        app.submissionService.start(sv1, sid);
        app.submissionService.saveAnswer(sv1, sid, 2, "true");
        app.submissionService.start(sv2, sid);

        clock.advance(Duration.ofMinutes(55));   // đã qua giờ đóng
        assertThrows(SessionRuleException.class, () -> app.submissionService.saveAnswer(sv1, sid, 1, correctOf(1)));

        assertEquals(2, app.submissionService.closeExpiredSessions());
        Submission r = app.submissionService.resultOf(sv1, sid);
        assertEquals(EndReason.TIME_OUT, r.getEndReason());
        assertEquals(3.0, r.getTotalScore(), "chỉ câu đã lưu trước giờ đóng được tính");
        assertEquals(0, app.submissionService.closeExpiredSessions(), "chạy lại không nộp thêm bài nào");
    }

    @Test
    void đủNămLầnViPhạmThìKếtThúcBài() {
        app.submissionService.start(sv1, sid);
        app.submissionService.saveAnswer(sv1, sid, 1, correctOf(1));
        Instant t = Instant.EPOCH;
        ViolationResult r = null;
        for (int i = 0; i < 5; i++) {
            r = app.proctoring.report(sv1, sid, t.plusSeconds(10L * i));
        }
        assertTrue(r.terminated());
        Submission s = app.submissionService.resultOf(sv1, sid);
        assertTrue(s.isTerminatedByViolation());
        assertEquals(4.0, s.getTotalScore(), "bài vẫn được chấm phần đã làm");
        assertThrows(SessionRuleException.class, () -> app.submissionService.saveAnswer(sv1, sid, 2, "true"));
    }

    @Test
    void sựKiệnSátNhauĐượcGộpLàmMột() {
        app.submissionService.start(sv1, sid);
        Instant t = Instant.EPOCH;
        assertEquals(1, app.proctoring.report(sv1, sid, t).count());
        ViolationResult merged = app.proctoring.report(sv1, sid, t.plusMillis(800));
        assertTrue(merged.merged());
        assertEquals(1, merged.count());
        assertEquals(2, app.proctoring.report(sv1, sid, t.plusSeconds(5)).count());
    }

    @Test
    void thíSinhKhácNhauNhậnThứTựCâuKhácNhauNhưngỔnĐịnh() {
        var p1 = session.buildPaperFor(sv1).stream().map(i -> i.getOrder()).toList();
        var p1again = session.buildPaperFor(sv1).stream().map(i -> i.getOrder()).toList();
        assertEquals(p1, p1again, "vào lại vẫn đúng thứ tự cũ");
        // với 3 câu có 6 hoán vị, kiểm tra trên nhiều thí sinh giả để tránh trùng ngẫu nhiên
        long distinct = java.util.stream.IntStream.range(1000, 1010)
                .mapToObj(id -> session.buildPaperFor(new Student(id, "x" + id, "h$h", "", null, true, "C" + id))
                        .stream().map(i -> i.getOrder()).toList())
                .distinct().count();
        assertTrue(distinct > 1);
    }

    @Test
    void phânQuyềnĐượcKiểmTraỞTầngService() {
        assertThrows(AccessDeniedException.class, () -> app.submissionService.start(teacher, sid));
        assertThrows(AccessDeniedException.class, () -> app.statistics.statsFor(sv1, session));
        ExamAppException e = assertThrows(ExamAppException.class, () -> app.auth.login("gv", "sai"));
        assertEquals("INVALID_LOGIN", e.getCode());
        assertNotEquals(null, app.auth.login("gv", "g"));
    }

    @Test
    void đềĐãMởCaThìKhóaVàCâuHỏiKhôngXóaĐược() {
        assertTrue(exam.isLocked());
        Question used = exam.getItem(1).getQuestion();
        assertEquals("QUESTION_IN_USE", assertThrows(ExamAppException.class,
                () -> app.bankService.deleteQuestion(teacher, used)).getCode());
        assertFalse(app.questions.findById(used.getQuestionId()).isEmpty());
    }
}
