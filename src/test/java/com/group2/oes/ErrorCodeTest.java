package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.dao.memory.ManualClock;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.factory.QuestionFactory;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.ImportResult;
import com.group2.oes.service.NewAccount;
import com.group2.oes.ui.AppConfig.ServerSettings;
import com.group2.oes.ui.Formats;
import com.group2.oes.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.group2.oes.Fixtures.OOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mỗi mã lỗi trong chương trình có ít nhất một ca kiểm thử (Phase 7). Mã lỗi là "hợp đồng" giữa service và giao diện
 * (biểu mẫu câu hỏi dùng mã để biết hiện lỗi ở ô nào), nên đổi mã mà quên sửa giao diện thì kiểm thử phải báo.
 */
class ErrorCodeTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);

    private AppContext app;
    private ClientContext client;
    private Administrator admin;
    private Instructor teacher;
    private Subject oop;

    @TempDir
    Path tmp;

    @BeforeEach
    void setUp() {
        InMemoryDatabase db = new InMemoryDatabase(new ManualClock(T0));
        app = AppContext.inMemory(db);
        client = ClientContext.inMemory(db);
        admin = app.users.insert(new Administrator(0, "admin", PasswordHasher.hash("admin123"), "QTV", null, true));
        oop = app.subjectService.createSubject(admin, "INT1332", "OOP");
        teacher = app.auth.createUser(admin, new Instructor(0, "gv", PasswordHasher.hash("gv1234"), "GV", null, true));
        app.subjectService.assignInstructor(admin, teacher, oop);
    }

    private static void code(String expected, Executable action) {
        assertEquals(expected, assertThrows(ExamAppException.class, action).getCode());
    }

    @Test
    void lỗiDữLiệuCâuHỏi() {
        QuestionData base = QuestionData.basic(OOP, "Câu", 1, Difficulty.EASY, 1);
        code("CHAPTER_INVALID", () -> QuestionFactory.create(QuestionType.TRUE_FALSE,
                QuestionData.basic(OOP, "Câu", 0, Difficulty.EASY, 1).withCorrectValue(true)));
        code("OPTION_EMPTY", () -> AnswerOption.of("  ", true));
        code("OPTIONS_TOO_FEW", () -> QuestionFactory.create(QuestionType.SINGLE_CHOICE,
                base.withOptions(List.of(AnswerOption.of("A", true)))));
        code("MULTI_NEEDS_CORRECT", () -> QuestionFactory.create(QuestionType.MULTI_CHOICE,
                base.withOptions(List.of(AnswerOption.of("A", false), AnswerOption.of("B", false)))));
        code("FILL_NEEDS_ANSWER", () -> QuestionFactory.create(QuestionType.FILL_IN_BLANK, base.withAccepted(List.of(" "), false)));
        code("QUESTION_NOT_SAVED", () -> new QuestionBank(OOP).add(Fixtures.trueFalse(0, true)));
        code("QUESTION_NOT_FOUND", () -> new QuestionBank(OOP).get(42));
        code("QUESTION_NOT_FOUND", () -> app.bankService.updateQuestion(teacher, QuestionType.TRUE_FALSE,
                QuestionData.basic(oop, "x", 1, Difficulty.EASY, 1).withCorrectValue(true).withId(99999)));
    }

    @Test
    void lỗiDữLiệuĐềVàMaTrận() {
        Question q = Fixtures.trueFalse(5, true);
        code("EXAM_TITLE", () -> Exam.draft(" ", OOP, MultiChoicePolicy.PARTIAL));
        code("ITEM_ORDER", () -> new ExamItem(0, 1, q));
        code("ITEM_POINT", () -> new ExamItem(1, 0, q));
        code("ITEM_NOT_FOUND", () -> Exam.draft("Đề", OOP, MultiChoicePolicy.PARTIAL).getItem(1));
        code("MATRIX_COUNT", () -> new ExamMatrix().add(1, Difficulty.EASY, 0));
        code("MATRIX_DUPLICATE", () -> new ExamMatrix().add(1, Difficulty.EASY, 1).add(1, Difficulty.EASY, 2));
        code("BANK_SHORTAGE", () -> new QuestionBank(OOP).pick(new ExamMatrix().add(1, Difficulty.HARD, 1), new Random(1)));
    }

    @Test
    void lỗiPhânQuyềnVàTàiKhoản() {
        Instructor other = app.auth.createUser(admin, new Instructor(0, "gv2", PasswordHasher.hash("gv1234"), "GV2", null, true));
        code("SUBJECT_FORBIDDEN", () -> app.bankService.loadBank(other, oop));
        code("ROLE_REQUIRED", () -> app.auth.createUser(teacher, new Instructor(0, "x", "h$h", "X", null, true)));
        code("ROLE_REQUIRED", () -> client.exams.enter(teacher, 1));
        code("NOT_LOGGED_IN", () -> app.auth.changePassword(null, "a", "bbbbbb"));
        code("USERNAME_TAKEN", () -> app.auth.createUser(admin, new Instructor(0, "gv", "h$h", "Trùng", null, true)));
        code("ROLE_EMPTY", () -> app.auth.createAccount(admin, new NewAccount(null, "abc", "matkhau1", "X", null, null, null)));
        code("PASSWORD_MISMATCH", () -> User.requireConfirmed("matkhau1", "matkhau2"));
    }

    @Test
    void lỗiCaThiVàBàiLàm() {
        Student sv = app.auth.createUser(admin, new Student(0, "sv", PasswordHasher.hash("s"), "SV", null, true, "B1"));
        Question q = app.bankService.addQuestion(teacher, QuestionType.TRUE_FALSE,
                QuestionData.basic(oop, "Đúng?", 1, Difficulty.EASY, 1).withCorrectValue(true));
        Exam exam = app.examService.createManual(teacher, app.bankService.loadBank(teacher, oop), "Đề",
                MultiChoicePolicy.PARTIAL, Map.of(q.getQuestionId(), 1.0));
        code("SESSION_IN_PAST", () -> app.sessionService.createSession(teacher, exam,
                new SessionWindow(T0.minusHours(2), T0.minusHours(1)), new SessionPolicy(true, true, true), List.of(sv)));
        code("SESSION_NOT_FOUND", () -> client.exams.enter(sv, 99999));
        code("SESSION_NOT_FOUND", () -> app.sessionService.get(99999));
        var s = app.sessionService.createSession(teacher, exam, new SessionWindow(T0, T0.plusHours(1)),
                new SessionPolicy(true, true, true), List.of(sv));
        code("NOT_STARTED", () -> client.exams.refresh(sv, s.getSessionId()));
    }

    @Test
    void lỗiNhậpTệpGhiTheoDòng() throws IOException {
        Path f = tmp.resolve("cau-hoi.txt");
        Files.writeString(f, "TRUE_FALSE|1|EASY|1|Câu đúng sai|có lẽ\nSINGLE_CHOICE|1|EASY|1|Thiếu cột\n");
        ImportResult r = app.bankService.importFile(teacher, oop, f);
        assertEquals(0, r.imported());
        assertTrue(r.errors().get(0).contains("true hoặc false"), "TF_VALUE");
        assertTrue(r.errors().get(1).contains("cần 6 trường"), "CSV_COLUMNS");

        Path csv = tmp.resolve("sv.csv");
        Files.writeString(csv, "B24001,Thiếu cột\n");
        assertTrue(app.userImport.importStudents(admin, csv).errors().get(0).contains("cần 4 cột"), "CSV_COLUMNS");
    }

    @Test
    void lỗiÔNhậpCủaGiaoDiện() {
        code("DATE_EMPTY", () -> Formats.combine(null, "07:30", "Giờ mở"));
        code("TIME_INVALID", () -> Formats.combine(LocalDate.of(2026, 11, 20), "7h30", "Giờ mở"));
        assertEquals(LocalDateTime.of(2026, 11, 20, 7, 30), Formats.combine(LocalDate.of(2026, 11, 20), " 7:30 ", "Giờ mở"));
        code("HOST_INVALID", () -> new ServerSettings(" ", 3306, "oes", "oes_staff", ""));
        code("PORT_INVALID", () -> new ServerSettings("10.0.0.1", 0, "oes", "oes_staff", ""));
        code("DB_INVALID", () -> new ServerSettings("10.0.0.1", 3306, "oes; DROP", "oes_staff", ""));
        code("USER_INVALID", () -> new ServerSettings("10.0.0.1", 3306, "oes", " ", ""));
        assertTrue(new ServerSettings("10.0.0.1", 3306, "oes", "oes_client", "x").jdbcUrl().contains("connectTimeout"));
    }
}
