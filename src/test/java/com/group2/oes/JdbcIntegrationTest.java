package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.factory.QuestionFactory;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Role;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.Submission;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.MultiChoiceQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.ImportResult;
import com.group2.oes.util.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Chạy với MySQL/MariaDB thật. Đặt biến môi trường OES_TEST_DB trỏ tới tệp properties (tài khoản oes_staff)
 * của một CSDL đã chạy db/schema.sql và db/security.sql. Không có biến này thì các kiểm thử được bỏ qua.
 *
 * Ở đây phía thí sinh cũng dùng tài khoản oes_staff, để kiểm tra riêng phần JDBC;
 * kiểm thử phân quyền bằng tài khoản oes_client nằm ở JdbcSecurityTest.
 */
class JdbcIntegrationTest {

    private static AppContext app;
    private static ClientContext client;
    private static ConnectionManager cm;
    private static String sfx;

    @BeforeAll
    static void connect() {
        String path = System.getenv("OES_TEST_DB");
        assumeTrue(path != null && Files.exists(Path.of(path)), "Bỏ qua: chưa đặt OES_TEST_DB");
        cm = ConnectionManager.fromFile(Path.of(path));
        app = AppContext.jdbc(cm);
        client = ClientContext.jdbc(cm);
        sfx = "_t" + System.nanoTime() % 1_000_000;
    }

    private record World(Instructor teacher, Subject subject, Student s1, Student s2, Exam exam, ExamSession session) {
    }

    private World world() {
        String tag = sfx + "_" + (int) (Math.random() * 1e6);
        Administrator admin = app.users.insert(new Administrator(0, "adm" + tag, PasswordHasher.hash("a"), "A", null, true));
        Subject subj = app.subjects.insert("S" + tag, "Môn kiểm thử");
        Instructor t = app.auth.createUser(admin, new Instructor(0, "gv" + tag, PasswordHasher.hash("g"), "GV", null, true));
        app.subjects.assignInstructor(t, subj);
        Student s1 = app.auth.createUser(admin, new Student(0, "a" + tag, PasswordHasher.hash("s"), "Đỗ Thị Vân Anh", null, true, "A" + tag));
        Student s2 = app.auth.createUser(admin, new Student(0, "b" + tag, PasswordHasher.hash("s"), "Phan Tùng Dương", null, true, "B" + tag));

        QuestionData base = QuestionData.basic(subj, "Câu hỏi", 1, Difficulty.EASY, 1);
        Question q1 = app.bankService.addQuestion(t, QuestionType.MULTI_CHOICE, base.withOptions(List.of(
                AnswerOption.of("A", true), AnswerOption.of("B", true), AnswerOption.of("C", false))));
        Question q2 = app.bankService.addQuestion(t, QuestionType.FILL_IN_BLANK, base.withAccepted(List.of("đóng gói"), false));
        QuestionBank bank = app.bankService.loadBank(t, subj);
        Map<Integer, Double> picks = new LinkedHashMap<>();
        picks.put(q1.getQuestionId(), 6.0);
        picks.put(q2.getQuestionId(), 4.0);
        Exam exam = app.examService.createManual(t, bank, "Đề JDBC", MultiChoicePolicy.PARTIAL, picks);
        LocalDateTime now = app.sessions.serverTime();
        ExamSession ses = app.sessionService.createSession(t, exam,
                new SessionWindow(now.minusMinutes(1), now.plusMinutes(30)),
                new SessionPolicy(false, false, true), List.of(s1, s2));
        return new World(t, subj, s1, s2, exam, ses);
    }

    @Test
    void đọcCâuHỏiLênĐúngLớpConQuaFactory() {
        World w = world();
        List<Question> loaded = app.questions.findBySubject(w.subject());
        assertEquals(2, loaded.size());
        assertInstanceOf(MultiChoiceQuestion.class, loaded.get(0));
        assertInstanceOf(FillInBlankQuestion.class, loaded.get(1));
        assertEquals(3, loaded.get(0).getOptions().size());
        assertTrue(loaded.get(1).isCorrect("Đóng  Gói"), "chuỗi tiếng Việt đi qua CSDL vẫn so khớp đúng");
    }

    @Test
    void xóaCâuNằmTrongĐềBáoLỗiNghiệpVụKhôngPhảiLỗiKếtNối() {
        World w = world();
        Question inExam = w.exam().getItem(1).getQuestion();
        assertTrue(app.questions.isUsedInAnyExam(inExam.getQuestionId()));
        assertEquals("QUESTION_IN_USE", assertThrows(ValidationException.class,
                () -> app.bankService.deleteQuestion(w.teacher(), inExam)).getCode());

        Question free = app.bankService.addQuestion(w.teacher(), QuestionType.TRUE_FALSE,
                QuestionData.basic(w.subject(), "Câu tự do", 1, Difficulty.EASY, 1).withCorrectValue(true));
        assertFalse(app.questions.isUsedInAnyExam(free.getQuestionId()));
        app.bankService.deleteQuestion(w.teacher(), free);
        assertTrue(app.questions.findById(free.getQuestionId()).isEmpty());
    }

    @Test
    void ràngBuộcDuyNhấtChặnBàiLàmThứHai() {
        World w = world();
        LocalDateTime now = client.studentExams.serverTime();
        int sid = w.session().getSessionId();
        client.studentExams.createAttempt(sid, w.s1().getUserId(), now);
        assertThrows(DuplicateKeyException.class,
                () -> client.studentExams.createAttempt(sid, w.s1().getUserId(), now));
    }

    @Test
    void haiMáyCùngBấmVàoThiChỉTạoMộtBài() throws Exception {
        World w = world();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> click = () -> client.exams.enter(w.s1(), w.session().getSessionId()).submissionId();
            Future<Integer> a = pool.submit(click);
            Future<Integer> b = pool.submit(click);
            assertEquals(a.get(), b.get(), "máy thứ hai mở lại đúng bài của máy thứ nhất");
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void đếmViPhạmSongSongKhôngMấtLượt() throws Exception {
        World w = world();
        int id = client.exams.enter(w.s2(), w.session().getSessionId()).submissionId();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> fs = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                fs.add(pool.submit(() -> client.studentExams.increaseViolation(id)));
            }
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> f : fs) {
                results.add(f.get());
            }
            results.sort(Integer::compare);
            assertEquals(List.of(1, 2, 3, 4), results, "mỗi luồng nhận một giá trị khác nhau");
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void nộpVàChấmQuaCSDL() {
        World w = world();
        int sid = w.session().getSessionId();
        ExamAttempt a = client.exams.enter(w.s1(), sid);
        Question multi = w.exam().getItem(1).getQuestion();
        int correctA = multi.getOptions().get(0).getOptionId();
        client.exams.saveAnswer(w.s1(), a, 1, String.valueOf(correctA));   // đúng 1/2
        client.exams.saveAnswer(w.s1(), a, 2, "ĐÓNG GÓI");
        client.exams.setFlag(w.s1(), a, 2, true);
        client.exams.submit(w.s1(), a, EndReason.MANUAL);
        assertNull(client.exams.refresh(w.s1(), sid).getTotalScore(), "chưa chấm thì chưa có điểm");

        assertTrue(app.gradingWorker.runOnce().graded() >= 1);
        AttemptState graded = client.exams.refresh(w.s1(), sid);
        assertEquals(7.0, graded.getTotalScore(), "6 x 0.5 + 4");
        assertTrue(graded.isFlagged(2), "cờ đánh dấu đi qua CSDL");

        Submission reloaded = app.submissions.findAllOfSession(w.session()).stream()
                .filter(s -> s.getStudent().equals(w.s1())).findFirst().orElseThrow();
        assertEquals(3.0, reloaded.getAnswer(1).getEarnedPoint());
        assertFalse(app.grading.saveGrades(reloaded), "bài đã chấm không bị ghi đè");
    }

    @Test
    void máyChấmNộpBàiQuáGiờBằngMộtCâuLệnh() {
        World w = world();
        int sid = w.session().getSessionId();
        client.exams.enter(w.s1(), sid);
        client.exams.enter(w.s2(), sid);
        // đẩy giờ đóng về quá khứ: createSession không cho tạo ca đã đóng
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE exam_session SET open_time = NOW() - INTERVAL 2 HOUR,"
                    + " close_time = NOW() - INTERVAL 1 SECOND WHERE id = ?")) {
                ps.setInt(1, sid);
                return ps.executeUpdate();
            }
        });
        app.gradingWorker.runOnce();
        AttemptState s = client.exams.refresh(w.s2(), sid);
        assertEquals(EndReason.TIME_OUT, s.getEndReason());
        assertEquals(0.0, s.getTotalScore());
    }

    @Test
    void giaoDịchLỗiGiữaChừngThìKhôngGhiGìCả() {
        World w = world();
        int before = app.questions.findBySubject(w.subject()).size();
        // Phương án đầu mang sẵn mã 999, nên assignId lúc lưu sẽ ném IllegalStateException
        // giữa giao dịch, sau khi dòng question đã được chèn. Dòng đó phải bị rollback.
        QuestionData data = QuestionData.basic(w.subject(), "Rollback", 1, Difficulty.EASY, 1)
                .withOptions(List.of(new AnswerOption(999, "đã có mã", true), AnswerOption.of("B", false)));
        Question q = QuestionFactory.create(QuestionType.SINGLE_CHOICE, data);
        assertThrows(IllegalStateException.class, () -> app.questions.insert(q));
        assertEquals(before, app.questions.findBySubject(w.subject()).size(), "câu hỏi đã bị rollback");
    }

    @Test
    void sửaCâuHỏiThayToànBộPhươngÁnTrongGiaoDịch() {
        World w = world();
        Question q = w.exam().getItem(1).getQuestion();   // câu nằm trong đề đã mở ca
        Question free = app.bankService.addQuestion(w.teacher(), QuestionType.SINGLE_CHOICE,
                QuestionData.basic(w.subject(), "Câu tự do", 1, Difficulty.EASY, 1)
                        .withOptions(List.of(AnswerOption.of("A", true), AnswerOption.of("B", false))));
        int oldOption = free.getOptions().get(0).getOptionId();
        app.bankService.updateQuestion(w.teacher(), QuestionType.MULTI_CHOICE,
                QuestionData.basic(w.subject(), "Câu đã sửa", 2, Difficulty.HARD, 2)
                        .withOptions(List.of(AnswerOption.of("X", true), AnswerOption.of("Y", true), AnswerOption.of("Z", false)))
                        .withId(free.getQuestionId()));
        Question reloaded = app.questions.findById(free.getQuestionId()).orElseThrow();
        assertInstanceOf(MultiChoiceQuestion.class, reloaded);
        assertEquals("Câu đã sửa", reloaded.getContent());
        assertEquals(3, reloaded.getOptions().size(), "phương án cũ bị xóa, phương án mới được chèn");
        assertTrue(reloaded.getOptions().stream().noneMatch(o -> o.getOptionId() == oldOption));
        assertEquals("QUESTION_IN_USE", assertThrows(ExamAppException.class, () -> app.bankService.updateQuestion(
                w.teacher(), QuestionType.TRUE_FALSE,
                QuestionData.basic(w.subject(), "x", 1, Difficulty.EASY, 1).withCorrectValue(true)
                        .withId(q.getQuestionId()))).getCode());
    }

    @Test
    void quảnLýNgườiDùngMônHọcVàLớpQuaCSDL() {
        World w = world();
        Administrator admin = app.users.insert(new Administrator(0, "ad2" + sfx + w.s1().getUserId(),
                PasswordHasher.hash("a"), "A", null, true));
        CourseClass cls = app.subjectService.createClass(admin, "L" + w.subject().getCode(), w.subject(), "HK1");
        assertEquals(cls.getClassId(), app.subjects.findClassByCode(cls.getCode()).orElseThrow().getClassId());
        CourseClass renamedClass = app.subjectService.updateClass(admin, cls, cls.getCode() + "X", "HK2");
        assertEquals(1, app.subjectService.listClasses(admin, w.subject()).size());

        app.auth.updateUser(admin, w.s1(), "Đỗ Thị Vân Anh", "anh@ptit.edu.vn", renamedClass);
        assertEquals(List.of(w.s1()), app.users.findStudentsOfClass(renamedClass.getClassId()));
        Student reloaded = (Student) app.users.findById(w.s1().getUserId()).orElseThrow();
        assertEquals(renamedClass.getCode(), reloaded.getCourseClass().getCode(), "lớp đọc lên kèm mã lớp");
        assertEquals("anh@ptit.edu.vn", reloaded.getEmail());
        assertEquals(1, app.auth.searchUsers(admin, w.s1().getStudentCode(), Role.STUDENT).size());
        assertTrue(app.auth.searchUsers(admin, w.s1().getStudentCode(), Role.INSTRUCTOR).isEmpty());

        app.auth.resetPassword(admin, w.teacher(), "tam1234");
        assertTrue(app.auth.login(w.teacher().getUsername(), "tam1234").isMustChangePassword());

        assertTrue(app.subjects.isInUse(w.subject().getSubjectId()));
        assertEquals(List.of(w.teacher()), app.subjectService.instructorsOf(admin, w.subject()));
        app.subjectService.unassignInstructor(admin, w.teacher(), w.subject());
        assertTrue(app.subjectService.instructorsOf(admin, w.subject()).isEmpty());

        Subject empty = app.subjectService.createSubject(admin, "E" + w.subject().getCode(), "Môn trống");
        app.subjectService.assignInstructor(admin, w.teacher(), empty);
        app.subjectService.renameSubject(admin, empty, "Môn trống (đổi tên)");
        assertEquals("Môn trống (đổi tên)", app.subjects.findByCode(empty.getCode()).orElseThrow().getName());
        app.subjectService.deleteSubject(admin, empty);
        assertTrue(app.subjects.findByCode(empty.getCode()).isEmpty(), "xóa được cả khi đã phân công giảng viên");
    }

    @Test
    void nhậpThíSinhTừCsvQuaCSDL() throws Exception {
        World w = world();
        Administrator admin = app.users.insert(new Administrator(0, "ad3" + sfx + w.s1().getUserId(),
                PasswordHasher.hash("a"), "A", null, true));
        String code = "C" + w.s1().getUserId() + "X";
        Path csv = Files.createTempFile("ds-", ".csv");
        Files.writeString(csv, "ma_sv,ho_ten,email,ma_lop\n" + code + ",Nguyễn Văn Nhập,,\n" + code + ",Trùng,,\n");
        ImportResult r = app.userImport.importStudents(admin, csv);
        assertEquals(1, r.imported());
        assertEquals(1, r.errors().size());
        assertTrue(client.auth.login(code.toLowerCase(), code.toLowerCase()).isMustChangePassword());
    }

    @Test
    void danhSáchĐềVàCaThiTheoMônVàTheoĐề() {
        World w = world();
        assertEquals(List.of(w.exam().getExamId()),
                app.examService.list(w.teacher(), w.subject()).stream().map(Exam::getExamId).toList());
        assertEquals(List.of(w.session().getSessionId()),
                app.sessionService.listOfExam(w.teacher(), w.exam()).stream().map(ExamSession::getSessionId).toList());
        Exam v2 = app.examService.newVersion(w.teacher(), w.exam());
        assertEquals(2, app.exams.findById(v2.getExamId()).orElseThrow().getVersion());
    }

    @Test
    void máyChủKhôngTrảLờiThìBáoLỗiSauVàiGiâyKhôngTreo() {
        // 10.255.255.1 không có máy nào: gói tin bị bỏ, không có trả lời từ chối
        ConnectionManager unreachable = new ConnectionManager("jdbc:mysql://10.255.255.1:3306/oes", "x", "y");
        long start = System.nanoTime();
        assertThrows(DataAccessException.class, unreachable::open);
        long seconds = (System.nanoTime() - start) / 1_000_000_000L;
        assertTrue(seconds < 15, "chờ " + seconds + " giây");
    }

    @Test
    void lỗiKếtNốiĐượcBọcThànhDataAccessException() {
        ConnectionManager bad = new ConnectionManager("jdbc:mysql://127.0.0.1:1/khong_co", "x", "y");
        DataAccessException e = assertThrows(DataAccessException.class, bad::open);
        assertInstanceOf(SQLException.class, e.getCause(), "nguyên nhân gốc vẫn còn để ghi log");
        assertNotNull(e.getUserMessage());
        assertFalse(e.getUserMessage().contains("jdbc"), "người dùng không thấy chi tiết kỹ thuật");
    }
}
