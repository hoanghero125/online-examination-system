package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.client.PaperItem;
import com.group2.oes.client.ReviewItem;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.dao.memory.ManualClock;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.SessionPhase;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.GradingWorker;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử các luồng nghiệp vụ chính qua tầng service, dùng DAO bộ nhớ và đồng hồ giả lập.
 * Hai "máy" dùng chung một CSDL bộ nhớ: máy giảng viên (app) chấm bài, máy thí sinh (client) chỉ làm bài.
 */
class SubmissionFlowTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);

    private ManualClock clock;
    private AppContext app;
    private ClientContext client;
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
        InMemoryDatabase db = new InMemoryDatabase(clock);
        app = AppContext.inMemory(db);
        client = ClientContext.inMemory(db);

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

        session = app.sessionService.createSession(teacher, exam, new SessionWindow(T0, T0.plusMinutes(60)),
                new SessionPolicy(true, true, true), List.of(sv1, sv2));
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
    void làmĐúngHếtĐượcTrọnĐiểmSauKhiMáyChấmChạy() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        for (int i = 1; i <= 3; i++) {
            client.exams.saveAnswer(sv1, a, i, correctOf(i));
        }
        AttemptState submitted = client.exams.submit(sv1, a, EndReason.MANUAL);
        assertEquals(SubmissionStatus.SUBMITTED, submitted.getStatus(), "máy thí sinh không chấm");
        assertNull(client.exams.refresh(sv1, sid).getTotalScore());

        assertEquals(1, app.gradingWorker.runOnce().graded());
        AttemptState graded = client.exams.refresh(sv1, sid);
        assertEquals(SubmissionStatus.GRADED, graded.getStatus());
        assertEquals(10.0, graded.getTotalScore());
    }

    @Test
    void mỗiThíSinhChỉThiMộtLần() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        client.exams.submit(sv1, a, EndReason.MANUAL);
        SessionRuleException e = assertThrows(SessionRuleException.class, () -> client.exams.enter(sv1, sid));
        assertEquals(SessionRuleException.ALREADY_SUBMITTED, e.getCode());
    }

    @Test
    void thoátRaVàoLạiThìMởĐúngBàiCũ() {
        ExamAttempt first = client.exams.enter(sv1, sid);
        client.exams.saveAnswer(sv1, first, 2, "true");
        client.exams.setFlag(sv1, first, 3, true);
        client.proctoring.report(sv1, first, Instant.EPOCH);

        ExamAttempt again = client.exams.enter(sv1, sid);
        assertEquals(first.submissionId(), again.submissionId());
        assertEquals("true", again.state().getResponse(2), "câu trả lời cũ còn nguyên");
        assertTrue(again.state().isFlagged(3), "câu đánh dấu xem lại còn nguyên");
        assertEquals(1, again.state().getViolationCount(), "số lần vi phạm không bị xóa khi vào lại");
    }

    @Test
    void nộpHaiLầnGiữKếtQuảLầnĐầu() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        client.exams.saveAnswer(sv1, a, 1, correctOf(1));
        client.exams.submit(sv1, a, EndReason.MANUAL);
        AttemptState b = client.exams.submit(sv1, a, EndReason.TIME_OUT);
        assertEquals(EndReason.MANUAL, b.getEndReason());

        app.gradingWorker.runOnce();
        assertEquals(4.0, client.exams.refresh(sv1, sid).getTotalScore());
        assertEquals(0, app.gradingWorker.runOnce().graded(), "bài đã chấm không bị chấm lại");
    }

    @Test
    void máyKhácĐãNộpThìĐọcLạiTrạngTháiThật() {
        ExamAttempt mayA = client.exams.enter(sv1, sid);
        ExamAttempt mayB = client.exams.enter(sv1, sid);
        client.exams.submit(sv1, mayA, EndReason.MANUAL);
        AttemptState fromB = client.exams.submit(sv1, mayB, EndReason.TIME_OUT);
        assertEquals(EndReason.MANUAL, fromB.getEndReason(), "kết quả của máy nộp trước được giữ");
    }

    @Test
    void ngoàiGiờHoặcKhôngCóTênThìKhôngVàoĐược() {
        clock.set(T0.minusMinutes(1));
        assertEquals(SessionRuleException.NOT_OPEN_YET,
                assertThrows(SessionRuleException.class, () -> client.exams.enter(sv1, sid)).getCode());
        clock.set(T0.plusMinutes(60));
        assertEquals(SessionRuleException.CLOSED,
                assertThrows(SessionRuleException.class, () -> client.exams.enter(sv1, sid)).getCode());
        clock.set(T0.plusMinutes(5));
        assertEquals(SessionRuleException.NOT_ENROLLED,
                assertThrows(SessionRuleException.class, () -> client.exams.enter(outsider, sid)).getCode());
    }

    @Test
    void hếtGiờThìKhôngGhiĐượcVàMáyChấmTựNộp() {
        ExamAttempt a1 = client.exams.enter(sv1, sid);
        client.exams.saveAnswer(sv1, a1, 2, "true");
        client.exams.enter(sv2, sid);

        clock.advance(Duration.ofMinutes(55));   // đã qua giờ đóng
        SessionRuleException e = assertThrows(SessionRuleException.class,
                () -> client.exams.saveAnswer(sv1, a1, 1, correctOf(1)));
        assertEquals(SessionRuleException.SUBMISSION_LOCKED, e.getCode());

        GradingWorker.RunResult r = app.gradingWorker.runOnce();
        assertEquals(2, r.closed());
        assertEquals(2, r.graded());
        AttemptState s = client.exams.refresh(sv1, sid);
        assertEquals(EndReason.TIME_OUT, s.getEndReason());
        assertEquals(3.0, s.getTotalScore(), "chỉ câu đã lưu trước giờ đóng được tính");
        assertFalse(app.gradingWorker.runOnce().hasWork(), "chạy lại không nộp hay chấm thêm bài nào");
    }

    @Test
    void đủNămLầnViPhạmThìKếtThúcBài() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        client.exams.saveAnswer(sv1, a, 1, correctOf(1));
        Instant t = Instant.EPOCH;
        ViolationResult r = null;
        for (int i = 0; i < 5; i++) {
            r = client.proctoring.report(sv1, a, t.plusSeconds(10L * i));
        }
        assertTrue(r.terminated());
        assertTrue(a.state().isLocked());

        app.gradingWorker.runOnce();
        AttemptState s = client.exams.refresh(sv1, sid);
        assertTrue(s.isTerminatedByViolation());
        assertEquals(5, s.getViolationCount());
        assertEquals(4.0, s.getTotalScore(), "bài vẫn được chấm phần đã làm");
        assertThrows(SessionRuleException.class, () -> client.exams.saveAnswer(sv1, a, 2, "true"));
    }

    @Test
    void sựKiệnSátNhauĐượcGộpLàmMột() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        Instant t = Instant.EPOCH;
        assertEquals(1, client.proctoring.report(sv1, a, t).count());
        ViolationResult merged = client.proctoring.report(sv1, a, t.plusMillis(800));
        assertTrue(merged.merged());
        assertEquals(1, merged.count());
        assertEquals(2, client.proctoring.report(sv1, a, t.plusSeconds(5)).count());
    }

    @Test
    void báoViPhạmKhiBàiĐãBịMáyChấmĐóngThìKhôngĐếmThêm() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        clock.advance(Duration.ofMinutes(56));
        app.gradingWorker.runOnce();   // máy chấm đóng ca trong khi máy thí sinh chưa biết

        ViolationResult r = client.proctoring.report(sv1, a, Instant.EPOCH);
        assertEquals(0, r.count(), "không hiện cảnh báo vi phạm giả");
        assertFalse(r.terminated(), "bài kết thúc do hết giờ, không phải do vi phạm");
        assertTrue(a.state().isLocked(), "màn hình biết bài đã khóa để chuyển sang kết quả");
        assertEquals(EndReason.TIME_OUT, a.state().getEndReason());
    }

    @Test
    void thíSinhKhácNhauNhậnThứTựCâuKhácNhauNhưngỔnĐịnh() {
        var p1 = session.buildPaperFor(sv1).stream().map(ExamItem::getOrder).toList();
        var p1again = session.buildPaperFor(sv1).stream().map(ExamItem::getOrder).toList();
        assertEquals(p1, p1again, "vào lại vẫn đúng thứ tự cũ");
        // với 3 câu có 6 hoán vị, kiểm tra trên nhiều thí sinh giả để tránh trùng ngẫu nhiên
        long distinct = java.util.stream.IntStream.range(1000, 1010)
                .mapToObj(id -> session.buildPaperFor(new Student(id, "x" + id, "h$h", "", null, true, "C" + id))
                        .stream().map(ExamItem::getOrder).toList())
                .distinct().count();
        assertTrue(distinct > 1);
    }

    @Test
    void máyThíSinhThấyCùngThứTựCâuVớiMáyGiảngViên() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        assertEquals(session.buildPaperFor(sv1).stream().map(ExamItem::getOrder).toList(),
                a.paper().getItems().stream().map(PaperItem::order).toList());
    }

    @Test
    void đápÁnChỉHiệnSauKhiCaĐóng() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        client.exams.saveAnswer(sv1, a, 2, "false");
        client.exams.submit(sv1, a, EndReason.MANUAL);
        app.gradingWorker.runOnce();

        List<ReviewItem> before = client.exams.review(sv1, sid);
        assertTrue(before.stream().noneMatch(ReviewItem::hasCorrectAnswer), "ca còn mở thì chưa lộ đáp án");
        assertEquals(0.0, before.get(1).earnedPoint(), "điểm từng câu có ngay sau khi chấm");

        clock.set(T0.plusMinutes(61));
        List<ReviewItem> after = client.exams.review(sv1, sid);
        assertTrue(after.stream().allMatch(ReviewItem::hasCorrectAnswer));
        assertEquals("Sai", after.get(1).responseText());
        assertEquals("Đúng", after.get(1).correctText());
        assertEquals("Đúng", after.get(0).correctText(), "mã phương án được đổi thành nội dung");
    }

    @Test
    void chưaNộpBàiThìChưaXemĐượcKếtQuả() {
        client.exams.enter(sv1, sid);
        assertEquals("NOT_SUBMITTED",
                assertThrows(SessionRuleException.class, () -> client.exams.review(sv1, sid)).getCode());
    }

    @Test
    void khôngThaoTácĐượcTrênBàiCủaNgườiKhác() {
        ExamAttempt a = client.exams.enter(sv1, sid);
        assertEquals("NOT_OWNER", assertThrows(AccessDeniedException.class,
                () -> client.exams.saveAnswer(sv2, a, 1, "x")).getCode());
        assertThrows(AccessDeniedException.class, () -> client.proctoring.report(sv2, a, Instant.EPOCH));
    }

    @Test
    void ứngDụngThiChỉNhậnTàiKhoảnThíSinh() {
        assertEquals("WRONG_APP",
                assertThrows(AccessDeniedException.class, () -> client.auth.login("gv", "g")).getCode());
        assertEquals("INVALID_LOGIN",
                assertThrows(ExamAppException.class, () -> client.auth.login("sv1", "sai")).getCode());
        assertEquals(sv1, client.auth.login("sv1", "s"));
    }

    @Test
    void phânQuyềnĐượcKiểmTraỞTầngService() {
        assertThrows(AccessDeniedException.class, () -> client.exams.enter(teacher, sid));
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

    @Test
    void danhSáchCaThiChiaTheoTrạngThái() {
        var groups = client.exams.sessionsOf(sv1);
        assertEquals(1, groups.get(SessionPhase.OPEN).size());
        clock.set(T0.plusHours(2));
        assertEquals(1, client.exams.sessionsOf(sv1).get(SessionPhase.CLOSED).size());
        assertTrue(client.exams.sessionsOf(outsider).values().stream().allMatch(List::isEmpty));
    }
}
