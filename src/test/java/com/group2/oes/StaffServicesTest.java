package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.dao.memory.ManualClock;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.ResultRow;
import com.group2.oes.model.Role;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.ImportResult;
import com.group2.oes.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Service phía quản trị viên và giảng viên (Phase 2): mỗi FR trong bảng kế hoạch có ít nhất một ca. */
class StaffServicesTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);

    private ManualClock clock;
    private AppContext app;
    private ClientContext client;
    private Administrator admin;
    private Instructor teacher;
    private Subject oop;
    private CourseClass class01;

    @TempDir
    Path tmp;

    @BeforeEach
    void setUp() {
        clock = new ManualClock(T0);
        InMemoryDatabase db = new InMemoryDatabase(clock);
        app = AppContext.inMemory(db);
        client = ClientContext.inMemory(db);
        admin = app.users.insert(new Administrator(0, "admin", PasswordHasher.hash("admin123"), "Quản trị", null, true));
        oop = app.subjectService.createSubject(admin, "INT1332", "Lập trình hướng đối tượng");
        class01 = app.subjectService.createClass(admin, "INT1332-01", oop, "HK1 2026");
        teacher = app.auth.createUser(admin, new Instructor(0, "gv", PasswordHasher.hash("gv1234"), "Giảng viên", null, true));
        app.subjectService.assignInstructor(admin, teacher, oop);
    }

    private Student student(String code, CourseClass cls) {
        Student s = new Student(0, code.toLowerCase(), PasswordHasher.hash("sv1234"), "SV " + code, null, true, code);
        s.setCourseClass(cls);
        return app.auth.createUser(admin, s);
    }

    private Path csv(String content) throws IOException {
        Path f = tmp.resolve("ds.csv");
        Files.writeString(f, content, StandardCharsets.UTF_8);
        return f;
    }

    // ------------------------------------------------------------ FR-01, FR-03: tài khoản

    @Test
    void tìmNgườiDùngTheoTừKhóaVàVaiTrò() {
        student("B24DCCE001", class01);
        student("B24DCCE002", null);
        assertEquals(2, app.auth.searchUsers(admin, null, Role.STUDENT).size());
        assertEquals(1, app.auth.searchUsers(admin, "ce001", null).size(), "tìm theo mã sinh viên, không phân biệt hoa thường");
        assertEquals(List.of(teacher), app.auth.searchUsers(admin, "giảng", null));
        assertThrows(AccessDeniedException.class, () -> app.auth.searchUsers(teacher, null, null));
    }

    @Test
    void quảnTrịViênSửaThôngTinVàLớpCủaThíSinh() {
        Student s = student("B24DCCE001", null);
        app.auth.updateUser(admin, s, "Nguyễn Văn An", "an@ptit.edu.vn", class01);
        Student reloaded = (Student) app.users.findById(s.getUserId()).orElseThrow();
        assertEquals("Nguyễn Văn An", reloaded.getFullName());
        assertEquals(List.of(s), app.users.findStudentsOfClass(class01.getClassId()));
        assertEquals("EMAIL_INVALID", assertThrows(ValidationException.class,
                () -> app.auth.updateUser(admin, s, "An", "khong-phai-email", null)).getCode());
        assertEquals("NAME_EMPTY", assertThrows(ValidationException.class,
                () -> app.auth.updateUser(admin, s, " ", null, null)).getCode());
    }

    @Test
    void khóaTàiKhoảnThìKhôngĐăngNhậpĐượcVàKhôngTựKhóaMình() {
        app.auth.setActive(admin, teacher, false);
        assertEquals("ACCOUNT_LOCKED", assertThrows(AccessDeniedException.class,
                () -> app.auth.login("gv", "gv1234")).getCode());
        assertEquals("SELF_LOCK", assertThrows(ValidationException.class,
                () -> app.auth.setActive(admin, admin, false)).getCode());
    }

    @Test
    void ứngDụngQuảnLýTừChốiTàiKhoảnThíSinh() {
        student("B24DCCE001", null);
        assertEquals("WRONG_APP", assertThrows(AccessDeniedException.class,
                () -> app.auth.login("b24dcce001", "sv1234")).getCode());
        assertEquals("INVALID_LOGIN", assertThrows(ValidationException.class,
                () -> app.auth.login("b24dcce001", "sai")).getCode(), "sai mật khẩu thì không lộ vai trò");
    }

    @Test
    void đặtLạiMậtKhẩuThìPhảiĐổiỞLầnĐăngNhậpSau() {
        app.auth.resetPassword(admin, teacher, "tam1234");
        User u = app.auth.login("gv", "tam1234");
        assertTrue(u.isMustChangePassword());
        app.auth.changePassword(u, "tam1234", "moi12345");
        assertFalse(app.auth.login("gv", "moi12345").isMustChangePassword());
    }

    // ------------------------------------------------------------ FR-04: hồ sơ, mật khẩu

    @Test
    void đổiMậtKhẩuPhảiĐúngMậtKhẩuCũVàĐủSáuKýTự() {
        User u = app.auth.login("gv", "gv1234");
        assertEquals("WRONG_PASSWORD", assertThrows(ValidationException.class,
                () -> app.auth.changePassword(u, "sai", "moi12345")).getCode());
        assertEquals("PASSWORD_TOO_SHORT", assertThrows(ValidationException.class,
                () -> app.auth.changePassword(u, "gv1234", "12345")).getCode());
        app.auth.changePassword(u, "gv1234", "123456");
        assertEquals(u, app.auth.login("gv", "123456"));
        assertThrows(ValidationException.class, () -> app.auth.login("gv", "gv1234"));
    }

    @Test
    void ngườiDùngTựSửaHồSơ() {
        User u = app.auth.login("gv", "gv1234");
        app.auth.updateProfile(u, "  Phạm Hoàng Việt ", "viet@ptit.edu.vn");
        User reloaded = app.users.findById(u.getUserId()).orElseThrow();
        assertEquals("Phạm Hoàng Việt", reloaded.getFullName());
        assertEquals("viet@ptit.edu.vn", reloaded.getEmail());
    }

    @Test
    void thíSinhĐổiMậtKhẩuQuaỨngDụngThi() {
        student("B24DCCE001", null);
        User sv = client.auth.login("b24dcce001", "sv1234");
        client.auth.changePassword(sv, "sv1234", "matkhaumoi");
        assertEquals(sv, client.auth.login("b24dcce001", "matkhaumoi"));
        assertThrows(ValidationException.class, () -> client.auth.login("b24dcce001", "sv1234"));
        assertEquals("WRONG_PASSWORD", assertThrows(ValidationException.class,
                () -> client.auth.changePassword(sv, "sai", "matkhau2")).getCode());
    }

    // ------------------------------------------------------------ FR-06: nhập thí sinh từ CSV

    @Test
    void nhậpThíSinhTừCsvGhiLỗiTheoDòng() throws IOException {
        student("B24DCCE009", null);
        Path f = csv("﻿ma_sv,ho_ten,email,ma_lop\n"
                + "B24DCCE001,Nguyễn Văn An,an@ptit.edu.vn,INT1332-01\n"
                + "b24dcce002,\"Trần, Thị Bình\",,\n"
                + "B24DCCE009,Trùng mã,,\n"
                + "B24DCCE003,Sai lớp,,KHONG-CO\n"
                + "B24DCCE004,Sai email,abc,\n"
                + "B24DCCE005,Thiếu cột\n"
                + "\n");
        ImportResult r = app.userImport.importStudents(admin, f);

        assertEquals(2, r.imported());
        assertEquals(4, r.errors().size(), r.errors().toString());
        assertTrue(r.errors().get(0).startsWith("Dòng 4:"), "số dòng tính cả dòng tiêu đề");
        assertTrue(r.errors().get(1).contains("KHONG-CO"));

        Student an = (Student) app.users.findByUsername("b24dcce001").orElseThrow();
        assertEquals(class01, an.getCourseClass());
        assertTrue(an.isMustChangePassword());
        Student binh = (Student) app.users.findByUsername("b24dcce002").orElseThrow();
        assertEquals("B24DCCE002", binh.getStudentCode(), "mã sinh viên viết hoa");
        assertEquals("Trần, Thị Bình", binh.getFullName(), "tên có dấu phẩy nằm trong nháy kép");

        User login = client.auth.login("b24dcce001", "b24dcce001");
        assertTrue(login.isMustChangePassword(), "ứng dụng thi biết phải bắt đổi mật khẩu");
        assertThrows(AccessDeniedException.class, () -> app.userImport.importStudents(teacher, f));
    }

    // ------------------------------------------------------------ FR-07: môn học, lớp, phân công

    @Test
    void quảnLýMônHọc() {
        assertEquals("SUBJECT_CODE_TAKEN", assertThrows(ValidationException.class,
                () -> app.subjectService.createSubject(admin, "INT1332", "Trùng")).getCode());
        assertEquals("CODE_INVALID", assertThrows(ValidationException.class,
                () -> app.subjectService.createSubject(admin, "INT 1", "Có dấu cách")).getCode());

        Subject db = app.subjectService.createSubject(admin, "INT1313", "CSDL");
        Subject renamed = app.subjectService.renameSubject(admin, db, "Cơ sở dữ liệu");
        assertEquals("Cơ sở dữ liệu", app.subjects.findByCode("INT1313").orElseThrow().getName());
        app.subjectService.deleteSubject(admin, renamed);
        assertTrue(app.subjects.findByCode("INT1313").isEmpty());

        assertEquals("SUBJECT_IN_USE", assertThrows(ValidationException.class,
                () -> app.subjectService.deleteSubject(admin, oop)).getCode(), "môn đã có lớp học phần");
        assertThrows(AccessDeniedException.class, () -> app.subjectService.createSubject(teacher, "X1", "X"));
    }

    @Test
    void quảnLýLớpHọcPhần() {
        CourseClass c2 = app.subjectService.createClass(admin, "INT1332-02", oop, "HK1 2026");
        assertEquals("CLASS_CODE_TAKEN", assertThrows(ValidationException.class,
                () -> app.subjectService.createClass(admin, "INT1332-01", oop, null)).getCode());
        CourseClass updated = app.subjectService.updateClass(admin, c2, "INT1332-03", "HK2 2026");
        assertEquals("HK2 2026", app.subjects.findClassByCode("INT1332-03").orElseThrow().getSemester());
        assertEquals(2, app.subjectService.listClasses(teacher, oop).size(), "giảng viên phụ trách môn xem được lớp");
        assertEquals(updated.getClassId(), c2.getClassId());
    }

    @Test
    void phânCôngGiảngViênQuyếtĐịnhQuyềnTrênMôn() {
        Subject db = app.subjectService.createSubject(admin, "INT1313", "CSDL");
        assertEquals(List.of(oop), app.subjectService.listSubjects(teacher));
        assertThrows(AccessDeniedException.class, () -> app.bankService.loadBank(teacher, db));

        app.subjectService.assignInstructor(admin, teacher, db);
        assertEquals(List.of(teacher), app.subjectService.instructorsOf(admin, db));
        app.bankService.loadBank(teacher, db);

        app.subjectService.unassignInstructor(admin, teacher, db);
        assertThrows(AccessDeniedException.class, () -> app.bankService.loadBank(teacher, db));
        Instructor relogin = (Instructor) app.auth.login("gv", "gv1234");
        assertFalse(relogin.canManage(db), "đăng nhập lại cũng không còn quyền");
    }

    // ------------------------------------------------------------ FR-08: sửa câu hỏi

    private Question addSingle(String content) {
        return app.bankService.addQuestion(teacher, QuestionType.SINGLE_CHOICE,
                QuestionData.basic(oop, content, 1, Difficulty.EASY, 1)
                        .withOptions(List.of(AnswerOption.of("A", true), AnswerOption.of("B", false))));
    }

    @Test
    void sửaCâuHỏiKểCảĐổiLoại() {
        Question q = addSingle("Câu cũ");
        Question updated = app.bankService.updateQuestion(teacher, QuestionType.TRUE_FALSE,
                QuestionData.basic(oop, "Câu mới", 2, Difficulty.HARD, 2).withCorrectValue(false).withId(q.getQuestionId()));
        Question reloaded = app.questions.findById(q.getQuestionId()).orElseThrow();
        assertEquals("Câu mới", reloaded.getContent());
        assertEquals(QuestionType.TRUE_FALSE, reloaded.getType());
        assertEquals(updated.getQuestionId(), reloaded.getQuestionId());
    }

    @Test
    void khôngSửaCâuĐãDùngChoCaThiHoặcĐổiMôn() {
        Question q = addSingle("Câu dùng trong đề");
        Exam exam = app.examService.createManual(teacher, app.bankService.loadBank(teacher, oop), "Đề",
                MultiChoicePolicy.PARTIAL, Map.of(q.getQuestionId(), 1.0));
        Student s = student("B24DCCE001", class01);
        app.sessionService.createSession(teacher, exam, new SessionWindow(T0, T0.plusHours(1)),
                new SessionPolicy(false, false, false), List.of(s));

        QuestionData edit = QuestionData.basic(oop, "Sửa", 1, Difficulty.EASY, 1).withCorrectValue(true)
                .withId(q.getQuestionId());
        assertEquals("QUESTION_IN_USE", assertThrows(ValidationException.class,
                () -> app.bankService.updateQuestion(teacher, QuestionType.TRUE_FALSE, edit)).getCode());

        Question free = addSingle("Câu tự do");
        Subject other = app.subjectService.createSubject(admin, "INT1313", "CSDL");
        app.subjectService.assignInstructor(admin, teacher, other);
        QuestionData moved = QuestionData.basic(other, "Chuyển môn", 1, Difficulty.EASY, 1).withCorrectValue(true)
                .withId(free.getQuestionId());
        assertEquals("SUBJECT_CHANGED", assertThrows(ValidationException.class,
                () -> app.bankService.updateQuestion(teacher, QuestionType.TRUE_FALSE, moved)).getCode());
    }

    // ------------------------------------------------------------ FR-19, BR-09: đề, ca thi, ghi danh

    private Exam smallExam() {
        Question q1 = addSingle("Câu 1");
        Question q2 = addSingle("Câu 2");
        Map<Integer, Double> picks = new LinkedHashMap<>();
        picks.put(q1.getQuestionId(), 4.0);
        picks.put(q2.getQuestionId(), 6.0);
        return app.examService.createManual(teacher, app.bankService.loadBank(teacher, oop), "Giữa kỳ",
                MultiChoicePolicy.PARTIAL, picks);
    }

    @Test
    void ghiDanhCảLớpHọcPhần() {
        Student a = student("B24DCCE001", class01);
        Student b = student("B24DCCE002", class01);
        student("B24DCCE003", null);
        Exam exam = smallExam();
        ExamSession s = app.sessionService.createSessionForClass(teacher, exam,
                new SessionWindow(T0, T0.plusHours(1)), new SessionPolicy(true, true, true), class01);
        assertEquals(Set.of(a.getUserId(), b.getUserId()), s.getEnrolledStudentIds());
        assertEquals(List.of(s), app.sessionService.listOfExam(teacher, exam));
        assertEquals(3, app.sessionService.allStudents(teacher).size());

        Subject other = app.subjectService.createSubject(admin, "INT1313", "CSDL");
        CourseClass otherClass = app.subjectService.createClass(admin, "INT1313-01", other, null);
        assertEquals("CLASS_WRONG_SUBJECT", assertThrows(ValidationException.class,
                () -> app.sessionService.createSessionForClass(teacher, exam, new SessionWindow(T0, T0.plusHours(1)),
                        new SessionPolicy(true, true, true), otherClass)).getCode());
    }

    @Test
    void lớpChưaCóThíSinhThìKhôngMởCaĐược() {
        Exam exam = smallExam();
        assertEquals("NO_STUDENTS", assertThrows(ValidationException.class,
                () -> app.sessionService.createSessionForClass(teacher, exam, new SessionWindow(T0, T0.plusHours(1)),
                        new SessionPolicy(true, true, true), class01)).getCode());
    }

    @Test
    void sinhĐềTheoMaTrậnKhôngLưuĐềRỗng() {
        addSingle("Câu 1");
        QuestionBank bank = app.bankService.loadBank(teacher, oop);
        assertEquals("EXAM_EMPTY", assertThrows(ValidationException.class, () -> app.examService.generate(teacher, bank,
                "Rỗng", MultiChoicePolicy.PARTIAL, new ExamMatrix(), 1.0, new Random(1))).getCode());
        assertTrue(app.examService.list(teacher, oop).isEmpty(), "không đề nào được lưu");
    }

    @Test
    void khôngXóaCâuĐangNằmTrongĐềChưaKhóa() {
        Exam exam = smallExam();
        Question inExam = exam.getItem(1).getQuestion();
        assertEquals("QUESTION_IN_USE", assertThrows(ValidationException.class,
                () -> app.bankService.deleteQuestion(teacher, inExam)).getCode());
        assertEquals(2, app.examService.list(teacher, oop).get(0).getItems().size(), "đề không bị mất câu");

        Question free = addSingle("Câu tự do");
        app.bankService.deleteQuestion(teacher, free);
        assertTrue(app.questions.findById(free.getQuestionId()).isEmpty());
    }

    @Test
    void danhSáchĐềVàTạoPhiênBảnMớiChoĐềĐãKhóa() {
        Exam exam = smallExam();
        exam.lock();
        Exam v2 = app.examService.newVersion(teacher, exam);
        assertEquals(2, v2.getVersion());
        assertFalse(v2.isLocked());
        assertEquals(10.0, v2.getTotalPoint());
        assertEquals(2, app.examService.list(teacher, oop).size());
    }

    // ------------------------------------------------------------ FR-30, FR-34, FR-35: bảng kết quả

    @Test
    void bảngKếtQuảCóCảBàiChưaChấmVàĐánhDấuViPhạm() {
        Student a = student("B24DCCE001", class01);
        Student b = student("B24DCCE002", class01);
        Exam exam = smallExam();
        ExamSession session = app.sessionService.createSessionForClass(teacher, exam,
                new SessionWindow(T0, T0.plusHours(1)), new SessionPolicy(false, false, false), class01);
        clock.set(T0.plusMinutes(1));

        ExamAttempt aa = client.exams.enter(a, session.getSessionId());
        client.exams.saveAnswer(a, aa, 1, String.valueOf(exam.getItem(1).getQuestion().getOptions().get(0).getOptionId()));
        client.exams.submit(a, aa, EndReason.VIOLATION);
        client.exams.enter(b, session.getSessionId());
        app.gradingWorker.runOnce();

        List<ResultRow> rows = app.statistics.listResults(teacher, session);
        assertEquals(2, rows.size());
        ResultRow ra = rows.get(0);
        assertEquals("B24DCCE001", ra.studentCode());
        assertEquals(4.0, ra.score());
        assertEquals(4.0, ra.scoreOn10());
        assertTrue(ra.terminatedByViolation());
        ResultRow rb = rows.get(1);
        assertEquals(SubmissionStatus.IN_PROGRESS, rb.status());
        assertNull(rb.score(), "bài chưa chấm chưa có điểm");
        assertThrows(ExamAppException.class, () -> app.statistics.listResults(b, session), "thí sinh không xem bảng điểm");
    }
}
