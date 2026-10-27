package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.dao.memory.ManualClock;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.Role;
import com.group2.oes.model.SessionPhase;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionSummary;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.MultiChoiceQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.AuditLogService;
import com.group2.oes.service.NewAccount;
import com.group2.oes.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Các hàm service thêm cho màn hình quản trị (Phase 4) và giảng viên (Phase 5). */
class AdminAndResultsTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);

    private ManualClock clock;
    private AppContext app;
    private ClientContext client;
    private Administrator admin;
    private Instructor teacher;
    private Subject oop;

    @TempDir
    Path tmp;

    @BeforeEach
    void setUp() {
        clock = new ManualClock(T0);
        InMemoryDatabase db = new InMemoryDatabase(clock);
        app = AppContext.inMemory(db);
        client = ClientContext.inMemory(db);
        admin = app.users.insert(new Administrator(0, "admin", PasswordHasher.hash("admin123"), "QTV", null, true));
        oop = app.subjectService.createSubject(admin, "INT1332", "OOP");
        teacher = app.auth.createUser(admin, new Instructor(0, "gv", PasswordHasher.hash("gv1234"), "GV", null, true));
        app.subjectService.assignInstructor(admin, teacher, oop);
    }

    @Test
    void tạoTàiKhoảnTheoVaiTròDựngĐúngLớpCon() {
        User sv = app.auth.createAccount(admin, new NewAccount(Role.STUDENT, "B24DCCE050", "matkhau1", "Thí Sinh",
                "", "b24dcce050", null));
        assertInstanceOf(Student.class, sv);
        assertEquals("b24dcce050", sv.getUsername(), "tên đăng nhập viết thường");
        assertEquals("B24DCCE050", ((Student) sv).getStudentCode(), "mã sinh viên viết hoa");
        assertTrue(sv.isMustChangePassword(), "mật khẩu do quản trị viên đặt phải đổi ở lần đầu");
        assertInstanceOf(Instructor.class, app.auth.createAccount(admin,
                new NewAccount(Role.INSTRUCTOR, "gv2", "matkhau1", "GV 2", null, null, null)));

        assertEquals("USERNAME_INVALID", assertThrows(ValidationException.class, () -> app.auth.createAccount(admin,
                new NewAccount(Role.ADMIN, "có dấu", "matkhau1", "X", null, null, null))).getCode());
        assertEquals("PASSWORD_TOO_SHORT", assertThrows(ValidationException.class, () -> app.auth.createAccount(admin,
                new NewAccount(Role.ADMIN, "abc", "123", "X", null, null, null))).getCode());
        assertEquals("STUDENT_CODE_INVALID", assertThrows(ValidationException.class, () -> app.auth.createAccount(admin,
                new NewAccount(Role.STUDENT, "abcd", "matkhau1", "X", null, " ", null))).getCode());
        assertThrows(AccessDeniedException.class, () -> app.auth.createAccount(teacher,
                new NewAccount(Role.ADMIN, "hack", "matkhau1", "X", null, null, null)));
    }

    @Test
    void đặtLạiMậtKhẩuSinhMậtKhẩuTạmNgẫuNhiên() {
        String first = app.auth.resetPassword(admin, teacher);
        String second = app.auth.resetPassword(admin, teacher);
        assertEquals(8, second.length());
        assertNotEquals(first, second);
        assertTrue(second.matches("[a-z2-9]+") && !second.matches(".*[01loi].*"), "không có ký tự dễ nhầm");
        assertTrue(app.auth.login("gv", second).isMustChangePassword());
    }

    @Test
    void nhậtKýLấyDòngMớiNhấtTrướcVàLọcTheoTừKhóa() throws IOException {
        Path log = tmp.resolve("oes-audit.log");
        Files.write(log, List.of("1 | INFO | admin/ADMIN | LOGIN | admin", "2 | INFO | gv/INSTRUCTOR | LOGIN | gv",
                "", "3 | INFO | sv/STUDENT | TERMINATED_BY_VIOLATION | ca 5"));
        AuditLogService service = new AuditLogService(log);
        assertEquals(List.of("3 | INFO | sv/STUDENT | TERMINATED_BY_VIOLATION | ca 5",
                "2 | INFO | gv/INSTRUCTOR | LOGIN | gv"), service.recentLines(admin, null, 2));
        assertEquals(2, service.recentLines(admin, "login", 100).size());
        assertTrue(new AuditLogService(tmp.resolve("chua-co.log")).recentLines(admin, "", 10).isEmpty());
        assertThrows(AccessDeniedException.class, () -> service.recentLines(teacher, "", 10));
    }

    @Test
    void dữLiệuCâuHỏiCũĐiềnLạiĐượcVàoBiểuMẫu() {
        QuestionData base = QuestionData.basic(oop, "Chọn đúng", 2, Difficulty.HARD, 2);
        Question multi = app.bankService.addQuestion(teacher, QuestionType.MULTI_CHOICE, base.withOptions(List.of(
                AnswerOption.of("A", true), AnswerOption.of("B", false), AnswerOption.of("C", true))));
        QuestionData d = QuestionData.from(multi);
        assertEquals(multi.getQuestionId(), d.questionId());
        assertEquals(List.of(true, false, true), d.options().stream().map(AnswerOption::isCorrect).toList());
        assertTrue(d.options().stream().allMatch(o -> o.getOptionId() == 0), "phương án chép ra chưa có mã");
        Question saved = app.bankService.updateQuestion(teacher, QuestionType.MULTI_CHOICE, d);
        assertInstanceOf(MultiChoiceQuestion.class, saved);

        Question fill = app.bankService.addQuestion(teacher, QuestionType.FILL_IN_BLANK,
                base.withAccepted(List.of("Đóng Gói", "encapsulation"), false));
        QuestionData fd = QuestionData.from(fill);
        assertEquals(List.of("đóng gói", "encapsulation"), fd.acceptedAnswers(), "dạng đã chuẩn hóa");
        assertInstanceOf(FillInBlankQuestion.class, app.bankService.updateQuestion(teacher, QuestionType.FILL_IN_BLANK, fd));
    }

    @Test
    void tómTắtCaThiĐếmThíSinhĐãVàoVàĐãNộp() {
        Student a = app.auth.createUser(admin, new Student(0, "a", PasswordHasher.hash("s"), "A", null, true, "A1"));
        Student b = app.auth.createUser(admin, new Student(0, "b", PasswordHasher.hash("s"), "B", null, true, "B1"));
        Student c = app.auth.createUser(admin, new Student(0, "c", PasswordHasher.hash("s"), "C", null, true, "C1"));
        Question q = app.bankService.addQuestion(teacher, QuestionType.TRUE_FALSE,
                QuestionData.basic(oop, "Đúng?", 1, Difficulty.EASY, 1).withCorrectValue(true));
        Exam exam = app.examService.createManual(teacher, app.bankService.loadBank(teacher, oop), "Đề",
                MultiChoicePolicy.PARTIAL, Map.of(q.getQuestionId(), 1.0));
        ExamSession s = app.sessionService.createSession(teacher, exam, new SessionWindow(T0.plusMinutes(10), T0.plusMinutes(70)),
                new SessionPolicy(false, false, true), List.of(a, b, c));

        SessionSummary before = app.statistics.summarize(teacher, s, app.sessionService.serverTime());
        assertEquals(SessionPhase.UPCOMING, before.phase());
        assertEquals(3, before.enrolled());

        clock.set(T0.plusMinutes(20));
        ExamAttempt aa = client.exams.enter(a, s.getSessionId());
        client.exams.submit(a, aa, EndReason.MANUAL);
        client.exams.enter(b, s.getSessionId());
        SessionSummary during = app.statistics.summarize(teacher, s, clock.get());
        assertEquals(SessionPhase.OPEN, during.phase());
        assertEquals(2, during.started());
        assertEquals(1, during.submitted());
        assertThrows(AccessDeniedException.class, () -> app.statistics.summarize(a, s, clock.get()));
    }
}
