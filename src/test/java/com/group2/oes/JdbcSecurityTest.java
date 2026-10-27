package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.client.ReviewItem;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.DataAccessException;
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
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.util.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm chứng NFR-04 trên MySQL thật: máy thí sinh (tài khoản oes_client) không đọc được đáp án,
 * không tự cho mình điểm, không ghi được bài sau giờ đóng.
 *
 * Cần hai biến môi trường trỏ tới hai tệp properties của cùng một CSDL đã chạy schema.sql và security.sql:
 *   OES_TEST_DB        - tài khoản oes_staff (dựng dữ liệu, chấm bài)
 *   OES_TEST_DB_CLIENT - tài khoản oes_client
 * Thiếu một trong hai thì các kiểm thử được bỏ qua.
 */
class JdbcSecurityTest {

    private static ConnectionManager staffCm;
    private static ConnectionManager clientCm;
    private static AppContext staff;
    private static ClientContext client;

    @BeforeAll
    static void connect() {
        String staffPath = System.getenv("OES_TEST_DB");
        String clientPath = System.getenv("OES_TEST_DB_CLIENT");
        assumeTrue(staffPath != null && Files.exists(Path.of(staffPath)), "Bỏ qua: chưa đặt OES_TEST_DB");
        assumeTrue(clientPath != null && Files.exists(Path.of(clientPath)), "Bỏ qua: chưa đặt OES_TEST_DB_CLIENT");
        staffCm = ConnectionManager.fromFile(Path.of(staffPath));
        clientCm = ConnectionManager.fromFile(Path.of(clientPath));
        staff = AppContext.jdbc(staffCm);
        client = ClientContext.jdbc(clientCm);
    }

    private record World(Instructor teacher, Student student, Exam exam, ExamSession session) {
    }

    /** Dựng dữ liệu bằng tài khoản giảng viên: một câu một đáp án (2 điểm), một câu đúng/sai (3 điểm). */
    private World world() {
        String tag = "_s" + System.nanoTime() % 1_000_000_000L;
        Administrator admin = staff.users.insert(new Administrator(0, "adm" + tag, PasswordHasher.hash("a"), "A", null, true));
        Subject subj = staff.subjects.insert("S" + tag, "Môn kiểm thử bảo mật");
        Instructor t = staff.auth.createUser(admin, new Instructor(0, "gv" + tag, PasswordHasher.hash("g"), "GV", null, true));
        staff.subjects.assignInstructor(t, subj);
        Student s = staff.auth.createUser(admin,
                new Student(0, "sv" + tag, PasswordHasher.hash("sv-pass"), "Thí sinh", null, true, "SV" + tag));

        QuestionData base = QuestionData.basic(subj, "Câu hỏi bảo mật", 1, Difficulty.EASY, 1);
        Question q1 = staff.bankService.addQuestion(t, QuestionType.SINGLE_CHOICE, base.withOptions(List.of(
                AnswerOption.of("Đúng", true), AnswerOption.of("Sai", false))));
        Question q2 = staff.bankService.addQuestion(t, QuestionType.TRUE_FALSE, base.withCorrectValue(true));
        QuestionBank bank = staff.bankService.loadBank(t, subj);
        Map<Integer, Double> picks = new LinkedHashMap<>();
        picks.put(q1.getQuestionId(), 2.0);
        picks.put(q2.getQuestionId(), 3.0);
        Exam exam = staff.examService.createManual(t, bank, "Đề bảo mật", MultiChoicePolicy.PARTIAL, picks);
        LocalDateTime now = staff.sessions.serverTime();
        ExamSession ses = staff.sessionService.createSession(t, exam,
                new SessionWindow(now.minusMinutes(1), now.plusMinutes(30)),
                new SessionPolicy(true, true, true), List.of(s));
        return new World(t, s, exam, ses);
    }

    /** createSession không cho tạo ca đã đóng, nên dùng quyền giảng viên đẩy giờ đóng về quá khứ. */
    private static void closeSessionNow(int sessionId) {
        staffCm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE exam_session SET open_time = NOW() - INTERVAL 2 HOUR,"
                    + " close_time = NOW() - INTERVAL 1 SECOND WHERE id = ?")) {
                ps.setInt(1, sessionId);
                return ps.executeUpdate();
            }
        });
    }

    /** Chạy một câu SQL bằng tài khoản thí sinh, như khi ứng dụng thi bị sửa để gửi lệnh tùy ý. */
    private static void asClient(String sql, int param) {
        clientCm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (sql.contains("?")) {
                    ps.setInt(1, param);
                }
                if (sql.startsWith("SELECT")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? 1 : 0;
                    }
                }
                return ps.executeUpdate();
            }
        });
    }

    @Test
    void thíSinhKhôngĐọcĐượcBảngĐápÁn() {
        assertThrows(DataAccessException.class, () -> asClient("SELECT * FROM answer_option", 0));
        assertThrows(DataAccessException.class, () -> asClient("SELECT correct_value FROM question", 0));
        assertThrows(DataAccessException.class, () -> asClient("SELECT * FROM exam_item", 0));
        assertThrows(DataAccessException.class, () -> asClient("SELECT password_hash FROM app_user", 0));
    }

    @Test
    void thíSinhKhôngTựChoMìnhĐiểm() {
        World w = world();
        ExamAttempt a = client.exams.enter(w.student(), w.session().getSessionId());
        int id = a.submissionId();
        assertThrows(DataAccessException.class, () -> asClient("UPDATE submission SET total_score = 10 WHERE id = ?", id));
        assertThrows(DataAccessException.class, () -> asClient("UPDATE answer SET earned_point = 10 WHERE submission_id = ?", id));
        ExamAppException e = assertThrows(ExamAppException.class,
                () -> asClient("UPDATE submission SET status = 'GRADED' WHERE id = ?", id));
        assertEquals(SessionRuleException.SUBMISSION_LOCKED, e.getCode(), "trigger chặn tự chuyển sang GRADED");
        assertEquals(1, client.studentExams.increaseViolation(id));
        assertThrows(SessionRuleException.class,
                () -> asClient("UPDATE submission SET violation_count = 0 WHERE id = ?", id),
                "trigger chặn xóa số lần vi phạm");
    }

    @Test
    void ghiCâuTrảLờiSauGiờĐóngBịTriggerChặn() {
        World w = world();
        ExamAttempt a = client.exams.enter(w.student(), w.session().getSessionId());
        client.exams.saveAnswer(w.student(), a, 1, "1");

        closeSessionNow(w.session().getSessionId());
        // Gọi thẳng DAO, bỏ qua mọi kiểm tra của tầng service: chỉ còn trigger chặn
        SessionRuleException e = assertThrows(SessionRuleException.class,
                () -> client.studentExams.saveAnswer(a.submissionId(), 2, "true"));
        assertEquals(SessionRuleException.SUBMISSION_LOCKED, e.getCode());
        assertThrows(SessionRuleException.class, () -> client.studentExams.setFlag(a.submissionId(), 2, true));
    }

    @Test
    void bàiĐãNộpThìMáyThíSinhKhôngSửaĐược() {
        World w = world();
        ExamAttempt a = client.exams.enter(w.student(), w.session().getSessionId());
        client.exams.submit(w.student(), a, EndReason.MANUAL);
        assertThrows(SessionRuleException.class, () -> client.studentExams.saveAnswer(a.submissionId(), 1, "1"));
        assertThrows(SessionRuleException.class,
                () -> asClient("UPDATE submission SET status = 'IN_PROGRESS' WHERE id = ?", a.submissionId()));
    }

    @Test
    void luồngĐầyĐủQuaHaiTàiKhoảnVàĐápÁnChỉHiệnSauKhiĐóngCa() {
        World w = world();
        int sid = w.session().getSessionId();
        ExamAttempt a = client.exams.enter(w.student(), sid);
        assertEquals(2, a.paper().size(), "máy thí sinh đọc được đề qua khung nhìn");
        int correctOption = w.exam().getItem(1).getQuestion().getOptions().stream()
                .filter(AnswerOption::isCorrect).findFirst().orElseThrow().getOptionId();
        client.exams.saveAnswer(w.student(), a, 1, String.valueOf(correctOption));
        client.exams.saveAnswer(w.student(), a, 2, "false");
        assertEquals(SubmissionStatus.SUBMITTED, client.exams.submit(w.student(), a, EndReason.MANUAL).getStatus());

        staff.gradingWorker.runOnce();
        AttemptState graded = client.exams.refresh(w.student(), sid);
        assertEquals(SubmissionStatus.GRADED, graded.getStatus());
        assertEquals(2.0, graded.getTotalScore());

        List<ReviewItem> before = client.exams.review(w.student(), sid);
        assertTrue(before.stream().noneMatch(ReviewItem::hasCorrectAnswer), "ca còn mở thì v_review rỗng");
        assertEquals(2.0, before.get(0).earnedPoint());

        closeSessionNow(sid);
        List<ReviewItem> after = client.exams.review(w.student(), sid);
        assertEquals(String.valueOf(correctOption), after.get(0).correctAnswer());
        assertEquals("Đúng", after.get(1).correctText());
        assertEquals("Sai", after.get(1).responseText());
    }

    @Test
    void khungNhìnĐăngNhậpKhôngLộMãBămCủaGiảngViên() {
        World w = world();
        assertEquals("", client.logins.findByUsername(w.teacher().getUsername()).orElseThrow().getPasswordHash());
        assertEquals("WRONG_APP", assertThrows(AccessDeniedException.class,
                () -> client.auth.login(w.teacher().getUsername(), "g")).getCode());
        assertEquals(w.student(), client.auth.login(w.student().getUsername(), "sv-pass"));
    }

    @Test
    void thíSinhĐổiMậtKhẩuQuaThủTụcLưuTrữ() {
        World w = world();
        Administrator admin = staff.users.insert(new Administrator(0, "adm2" + w.student().getUsername(),
                PasswordHasher.hash("a"), "A", null, true));
        staff.auth.resetPassword(admin, w.student(), "tam1234");

        User sv = client.auth.login(w.student().getUsername(), "tam1234");
        assertTrue(sv.isMustChangePassword());
        client.auth.changePassword(sv, "tam1234", "matkhaumoi");
        User again = client.auth.login(w.student().getUsername(), "matkhaumoi");
        assertFalse(again.isMustChangePassword(), "thủ tục bỏ cờ buộc đổi mật khẩu");

        // quản trị viên vừa đặt lại mật khẩu trong lúc thí sinh đang đăng nhập: thủ tục so mã băm cũ và từ chối
        staff.auth.resetPassword(admin, w.student(), "tam5678");
        assertEquals("PASSWORD_CHANGED_ELSEWHERE", assertThrows(ExamAppException.class,
                () -> client.auth.changePassword(again, "matkhaumoi", "matkhau3")).getCode());

        // tài khoản thí sinh không đổi được mật khẩu của giảng viên, kể cả khi gọi thẳng thủ tục
        assertFalse(client.logins.changePassword(w.teacher().getUserId(), "", PasswordHasher.hash("hack123")));
        assertThrows(DataAccessException.class,
                () -> asClient("UPDATE app_user SET password_hash = 'x' WHERE id = ?", w.teacher().getUserId()));
    }

    @Test
    void thíSinhKhôngĐọcTrướcĐượcĐềCủaCaChưaMở() {
        World w = world();
        int examId = w.exam().getExamId();
        staffCm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE exam_session SET open_time = NOW() + INTERVAL 1 DAY,"
                    + " close_time = NOW() + INTERVAL 2 DAY WHERE id = ?")) {
                ps.setInt(1, w.session().getSessionId());
                return ps.executeUpdate();
            }
        });
        assertEquals(0, client.studentExams.loadPaper(examId).size());
        assertFalse(client.studentExams.findSession(w.session().getSessionId()).isEmpty(),
                "thông tin ca (giờ mở, số câu) vẫn xem được");
    }
}
