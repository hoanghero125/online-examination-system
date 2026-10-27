package com.group2.oes.ui;

import com.group2.oes.app.AppContext;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.util.PasswordHasher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Chạy thử giao diện không cần người bấm (chỉ dùng khi phát triển, không phải kiểm thử JUnit).
 * Chạy trong Xvfb, tự gõ, tự bấm, chụp màn hình bằng lệnh "import" của ImageMagick.
 *
 * <pre>
 *   xvfb-run java -cp ... com.group2.oes.ui.UiSmoke staff|client|deaddb &lt;config.properties&gt; &lt;thư-mục-ảnh&gt; [staff-config]
 * </pre>
 */
public final class UiSmoke {

    private static Path shots;
    private static Path staffConfig;

    private UiSmoke() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path config = Path.of(args[1]);
        if (mode.equals("exam-seed")) {
            seedExam(config, Integer.parseInt(args[2]));
            return;
        }
        shots = Files.createDirectories(Path.of(args[2]));
        staffConfig = args.length > 3 ? Path.of(args[3]) : config;
        if (mode.equals("staff") || mode.equals("client")) {
            seed(staffConfig);
        }
        Class<? extends Application> app = List.of("staff", "deaddb", "instructor", "admin").contains(mode)
                ? StaffApp.class : ExamClientApp.class;
        Thread fx = new Thread(() -> Application.launch(app, "--config=" + config));
        fx.setDaemon(true);
        fx.start();
        awaitToolkit();
        try {
            switch (mode) {
                case "staff" -> staff();
                case "client" -> client();
                case "exam" -> exam(args[4], args[5]);
                case "instructor" -> instructor(Integer.parseInt(args[4]));
                case "admin" -> admin();
                default -> deadDb();
            }
            System.out.println("UI SMOKE OK: " + mode);
        } catch (Throwable t) {
            shot(mode + "-failure");
            System.out.println("UI SMOKE FAILED: " + mode + " " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }
        Platform.exit();
        System.exit(0);
    }

    /** Ba tài khoản cố định cho lần chạy thử; chạy lại thì đặt lại mật khẩu. */
    private static void seed(Path staffConfig) {
        AppContext app = AppContext.jdbc(ConnectionManager.fromFile(staffConfig));
        ensure(app, new Administrator(0, "ui_admin", PasswordHasher.hash("admin123"), "Quản Trị Viên Thử", null, true), "admin123", false);
        Student sv = new Student(0, "ui_sv", PasswordHasher.hash("ui_sv123"), "Nguyễn Thị Thử", null, true, "UISV001");
        ensure(app, sv, "ui_sv123", true);
    }

    private static void ensure(AppContext app, User fresh, String password, boolean mustChange) {
        User u = app.users.findByUsername(fresh.getUsername()).orElseGet(() -> app.users.insert(fresh));
        u.changePassword(password);
        u.setActive(true);
        u.setMustChangePassword(mustChange);
        app.users.update(u);
    }

    /**
     * Đề 6 câu đủ 4 loại, một ca mở từ một phút trước tới {@code minutes} phút nữa, ghi danh ui_sv1, ui_sv2.
     * Các ca cũ của môn thử được đẩy về quá khứ để danh sách chỉ có một ca đang mở.
     */
    private static void seedExam(Path staffCfg, int minutes) {
        ConnectionManager cm = ConnectionManager.fromFile(staffCfg);
        AppContext app = AppContext.jdbc(cm);
        Administrator admin = (Administrator) ensureUser(app, new Administrator(0, "ui_admin", PasswordHasher.hash("admin123"),
                "Quản Trị Viên Thử", null, true), "admin123", false);
        Instructor gv = (Instructor) ensureUser(app, new Instructor(0, "ui_gv", PasswordHasher.hash("gv1234"),
                "Giảng Viên Thử", null, true), "gv1234", false);
        Student sv1 = (Student) ensureUser(app, new Student(0, "ui_sv1", PasswordHasher.hash("sv1234"),
                "Trần Văn Một", null, true, "UISV101"), "sv1234", false);
        Student sv2 = (Student) ensureUser(app, new Student(0, "ui_sv2", PasswordHasher.hash("sv1234"),
                "Lê Thị Hai", null, true, "UISV102"), "sv1234", false);
        Subject subject = app.subjects.findByCode("UIOOP").orElseGet(() -> app.subjects.insert("UIOOP", "Lập trình hướng đối tượng (thử)"));
        app.subjects.assignInstructor(gv, subject);
        cm.execute(c -> {
            try (var ps = c.prepareStatement("UPDATE exam_session SET open_time = NOW() - INTERVAL 3 HOUR,"
                    + " close_time = NOW() - INTERVAL 2 HOUR WHERE close_time > NOW()"
                    + " AND exam_id IN (SELECT id FROM exam WHERE subject_id = ?)")) {
                ps.setInt(1, subject.getSubjectId());
                return ps.executeUpdate();
            }
        });
        QuestionData base = QuestionData.basic(subject, "x", 1, Difficulty.EASY, 1);
        List<Question> qs = List.of(
                app.bankService.addQuestion(gv, QuestionType.SINGLE_CHOICE, QuestionData.basic(subject,
                        "Từ khóa nào dùng để tạo đối tượng mới trong Java?", 1, Difficulty.EASY, 1).withOptions(List.of(
                        AnswerOption.of("new", true), AnswerOption.of("create", false), AnswerOption.of("alloc", false)))),
                app.bankService.addQuestion(gv, QuestionType.MULTI_CHOICE, QuestionData.basic(subject,
                        "Những phát biểu nào đúng về đóng gói?", 2, Difficulty.MEDIUM, 2).withOptions(List.of(
                        AnswerOption.of("Thuộc tính thường để private", true), AnswerOption.of("Truy cập qua getter/setter", true),
                        AnswerOption.of("Mọi phương thức phải là static", false)))),
                app.bankService.addQuestion(gv, QuestionType.TRUE_FALSE, QuestionData.basic(subject,
                        "Lớp trừu tượng có thể có constructor.", 2, Difficulty.MEDIUM, 1).withCorrectValue(true)),
                app.bankService.addQuestion(gv, QuestionType.FILL_IN_BLANK, QuestionData.basic(subject,
                        "Từ khóa để một lớp kế thừa lớp khác là ...", 2, Difficulty.EASY, 1).withAccepted(List.of("extends"), false)),
                app.bankService.addQuestion(gv, QuestionType.SINGLE_CHOICE, QuestionData.basic(subject,
                        "Mẫu thiết kế nào cho phép thay đổi thuật toán lúc chạy?", 3, Difficulty.HARD, 1).withOptions(List.of(
                        AnswerOption.of("Singleton", false), AnswerOption.of("Strategy", true), AnswerOption.of("Builder", false)))),
                app.bankService.addQuestion(gv, QuestionType.TRUE_FALSE, base.withCorrectValue(false)));
        Map<Integer, Double> picks = new LinkedHashMap<>();
        qs.forEach(q -> picks.put(q.getQuestionId(), q.getType() == QuestionType.MULTI_CHOICE ? 2.0 : 1.5));
        Exam exam = app.examService.createManual(gv, app.bankService.loadBank(gv, subject), "Kiểm tra thử giao diện",
                MultiChoicePolicy.PARTIAL, picks);
        LocalDateTime now = app.sessions.serverTime();
        ExamSession s = app.sessionService.createSession(gv, exam, new SessionWindow(now.minusMinutes(1), now.plusMinutes(minutes)),
                new SessionPolicy(true, true, true), List.of(sv1, sv2));
        System.out.println("SEEDED session " + s.getSessionId() + " closes " + s.getWindow().closeTime());
    }

    private static User ensureUser(AppContext app, User fresh, String password, boolean mustChange) {
        ensure(app, fresh, password, mustChange);
        User u = app.users.findByUsername(fresh.getUsername()).orElseThrow();
        if (u instanceof Instructor ins) {
            app.subjects.loadSubjectsOf(ins);
        }
        return u;
    }

    // ------------------------------------------------------------ kịch bản

    private static void staff() throws Exception {
        waitFor("#usernameField");
        shot("staff-1-login");
        login("ui_admin", "admin123");
        waitFor("#userLabel");
        Thread.sleep(500);
        shot("staff-2-home-admin");
        System.out.println("  trang chủ: " + text("#userLabel") + " | " + text("#gradingLabel"));
        clickButton("Đăng xuất");
        waitForDialog();
        shot("staff-3-confirm-logout");
        clickDialogButton("Đăng xuất");
        waitFor("#usernameField");

        login("ui_sv", "ui_sv123");
        waitForDialog();
        System.out.println("  thí sinh đăng nhập ứng dụng quản lý: " + dialogText());
        shot("staff-4-student-rejected");
        clickDialogButton("Đóng");
    }

    private static void client() throws Exception {
        waitFor("#usernameField");
        login("ui_sv", "ui_sv123");
        waitFor("#confirmField");
        shot("client-1-forced-change");
        type("#oldField", "ui_sv123");
        type("#newField", "matkhau1");
        type("#confirmField", "khac123");
        clickButton("Lưu mật khẩu");
        waitForDialog();
        System.out.println("  nhập lại không khớp: " + dialogText());
        clickDialogButton("Đóng");
        type("#confirmField", "matkhau1");
        clickButton("Lưu mật khẩu");
        waitForDialog();
        clickDialogButton("Đóng");
        waitFor("#userLabel");
        Thread.sleep(300);
        shot("client-2-home");
        System.out.println("  trang chủ: " + text("#userLabel"));
        clickButton("Đăng xuất");
        waitForDialog();
        clickDialogButton("Đăng xuất");
        waitFor("#usernameField");
        login("ui_admin", "admin123");
        waitForDialog();
        System.out.println("  quản trị viên đăng nhập ứng dụng thi: " + dialogText());
        shot("client-3-admin-rejected");
        clickDialogButton("Đóng");
    }

    /**
     * Một thí sinh làm bài qua giao diện.
     *   full     : trả lời, đánh dấu, rời cửa sổ một lần, nộp, máy chấm chạy, xem kết quả
     *   violate5 : rời cửa sổ 5 lần cách nhau hơn 3 giây
     *   timeout  : trả lời một câu rồi chờ hết giờ
     */
    private static void exam(String user, String script) throws Exception {
        waitFor("#usernameField");
        login(user, "sv1234");
        waitFor("#openBox");
        Thread.sleep(800);
        shot(script + "-1-sessions");
        boolean resuming = onFx(() -> mainStage().getScene().getRoot().lookupAll(".button").stream()
                .anyMatch(n -> n instanceof ButtonBase b && "Tiếp tục làm bài".equals(b.getText())));
        clickButton(resuming ? "Tiếp tục làm bài" : "Vào thi");
        waitFor("#agreeBox");
        Thread.sleep(500);
        shot(script + "-2-rules");
        if (resuming) {
            System.out.println("  màn hình quy chế: " + text("#resumeLabel"));
        }
        onFx(() -> {
            ((javafx.scene.control.CheckBox) mainStage().getScene().getRoot().lookup("#agreeBox")).setSelected(true);
            return null;
        });
        clickButton(resuming ? "Tiếp tục làm bài" : "Bắt đầu làm bài");
        waitFor("#questionGrid");
        Thread.sleep(1500);
        System.out.println("  toàn màn hình: " + onFx(() -> mainStage().isFullScreen())
                + ", có tiêu điểm: " + onFx(() -> mainStage().isFocused()));
        if (script.equals("resume2")) {
            Thread.sleep(800);
            System.out.println("  số câu đã trả lời trên lưới sau khi mở lại: " + answeredOnGrid());
            shot("resume2-3-exam");
            return;
        }
        answerCurrent();
        shot(script + "-3-exam");

        switch (script) {
            case "full" -> {
                clickButton("Câu sau ▶");
                Thread.sleep(300);
                answerCurrent();
                clickButton("Đánh dấu xem lại");
                Thread.sleep(1200);
                shot("full-4-flagged");
                leaveWindow();
                waitForDialog();
                System.out.println("  vi phạm: " + dialogText());
                shot("full-5-violation-dialog");
                clickDialogButton("Quay lại làm bài");
                Thread.sleep(2000);
                System.out.println("  sau khi đóng hộp thoại: " + text("#violationLabel")
                        + ", toàn màn hình: " + onFx(() -> mainStage().isFullScreen())
                        + ", có tiêu điểm: " + onFx(() -> mainStage().isFocused()));
                clickButton("Nộp bài");
                waitForDialog();
                System.out.println("  xác nhận nộp: " + dialogText().replace('\n', ' '));
                shot("full-6-confirm-submit");
                clickDialogButton("Nộp bài");
                waitFor("#waitingLabel");
                Thread.sleep(800);
                shot("full-7-grading");
                System.out.println("  máy chấm: " + AppContext.jdbc(ConnectionManager.fromFile(staffConfig)).gradingWorker.runOnce());
                for (int i = 0; i < 50 && !onFx(() -> mainStage().getScene().getRoot().lookup("#resultBox").isVisible()); i++) {
                    Thread.sleep(200);
                }
                Thread.sleep(800);
                System.out.println("  kết quả: " + text("#scoreLabel") + " | " + text("#correctLabel") + " | " + text("#reviewNote"));
                shot("full-8-result");
            }
            case "reconnect" -> reconnect();
            case "resume1" -> {
                clickButton("Câu sau ▶");
                Thread.sleep(300);
                answerCurrent();
                Thread.sleep(2000);   // chờ hàng đợi lưu xong rồi "tắt máy" đột ngột
                System.out.println("  đã trả lời 2 câu, tắt ứng dụng đột ngột");
                Runtime.getRuntime().halt(0);
            }
            case "second" -> {
                // máy thứ hai của cùng tài khoản: không thao tác gì, phải tự biết bài đã nộp ở máy kia
                for (int i = 0; i < 150 && find("#waitingLabel").isEmpty() && find("#resultBox").isEmpty(); i++) {
                    Thread.sleep(500);
                }
                System.out.println("  máy thứ hai chuyển sang màn hình kết quả: "
                        + (find("#waitingLabel").isPresent() || find("#resultBox").isPresent()));
                shot("second-4-result");
            }
            case "violate5" -> {
                for (int i = 1; i <= 5; i++) {
                    leaveWindow();
                    if (i < 5) {
                        waitForDialog();
                        System.out.println("  " + dialogText());
                        clickDialogButton("Quay lại làm bài");
                        Thread.sleep(3300);   // hơn 3 giây: lần sau là một lần vi phạm mới
                    }
                }
                waitFor("#waitingLabel");
                System.out.println("  bài bị kết thúc, màn hình kết quả: " + text("#waitingLabel"));
                shot("violate5-4-terminated");
            }
            default -> {
                for (int i = 0; i < 600 && find("#waitingLabel").isEmpty(); i++) {
                    Thread.sleep(500);
                }
                waitFor("#waitingLabel");
                System.out.println("  hết giờ, màn hình kết quả: " + text("#waitingLabel"));
                shot("timeout-4-auto-submitted");
            }
        }
    }

    /**
     * Mất kết nối giữa giờ thi (Phase 7: "rút dây mạng"): dừng tạm mysqld bằng SIGSTOP, trả lời tiếp,
     * phải thấy dải vàng; cho mysqld chạy lại (SIGCONT), dải vàng phải tắt và câu trả lời phải có trong CSDL.
     */
    private static void reconnect() throws Exception {
        String pid = Files.readString(Path.of(System.getProperty("oes.mysqld.pid"))).strip();
        Thread.sleep(1500);   // câu 1 đã lưu xong
        new ProcessBuilder("kill", "-STOP", pid).start().waitFor();
        try {
            clickButton("Câu sau ▶");
            Thread.sleep(300);
            answerCurrent();
            for (int i = 0; i < 90 && !onFx(() -> mainStage().getScene().getRoot().lookup("#offlineBanner").isVisible()); i++) {
                Thread.sleep(500);
            }
            System.out.println("  mất kết nối, dải vàng hiện: "
                    + onFx(() -> mainStage().getScene().getRoot().lookup("#offlineBanner").isVisible()));
            shot("reconnect-4-offline");
            clickButton("Câu sau ▶");
            Thread.sleep(300);
            answerCurrent();   // vẫn làm bài tiếp được trong lúc mất mạng
        } finally {
            new ProcessBuilder("kill", "-CONT", pid).start().waitFor();
        }
        for (int i = 0; i < 90 && onFx(() -> mainStage().getScene().getRoot().lookup("#offlineBanner").isVisible()); i++) {
            Thread.sleep(500);
        }
        System.out.println("  có kết nối lại, dải vàng còn hiện: "
                + onFx(() -> mainStage().getScene().getRoot().lookup("#offlineBanner").isVisible()));
        shot("reconnect-5-online");
        ConnectionManager cm = ConnectionManager.fromFile(staffConfig);
        int saved = cm.execute(c -> {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM answer a JOIN submission s ON s.id = a.submission_id"
                    + " JOIN app_user u ON u.id = s.student_id WHERE u.username = 'ui_sv1' AND s.status = 'IN_PROGRESS'"
                    + " AND a.response IS NOT NULL")) {
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
        System.out.println("  số câu trả lời trong CSDL: " + saved + " (mong đợi 3)");
        if (saved != 3) {
            throw new IllegalStateException("mất câu trả lời khi mất kết nối");
        }
    }

    /** Giảng viên đi qua các màn hình Phase 5; {@code resultSession}: ca cần xem kết quả. */
    private static void instructor(int resultSession) throws Exception {
        waitFor("#usernameField");
        login("ui_gv", "gv1234");
        waitFor("#countLabel");
        Thread.sleep(1500);
        System.out.println("  ngân hàng: " + text("#countLabel"));
        shot("gv-1-bank");

        clickButton("Thêm câu hỏi");
        waitForStage("#contentArea");
        onStage("#contentArea", root -> {
            ((TextInputControl) root.lookup("#contentArea")).setText("Câu thử: chỉ có một phương án");
            return null;
        });
        clickIn("#contentArea", "Lưu câu hỏi");
        Thread.sleep(1500);
        System.out.println("  lỗi cạnh ô phương án: " + onStage("#contentArea",
                root -> ((Labeled) root.lookup("#optionsError")).getText()));
        shotStage("#contentArea", "gv-2-question-form-error");
        onStage("#contentArea", root -> {
            List<Node> rows = new java.util.ArrayList<>(((Parent) root.lookup("#optionRows")).getChildrenUnmodifiable());
            for (int i = 0; i < rows.size(); i++) {
                Parent row = (Parent) rows.get(i);
                ((TextInputControl) row.getChildrenUnmodifiable().get(0)).setText("Phương án " + (char) ('A' + i));
                ((javafx.scene.control.CheckBox) row.getChildrenUnmodifiable().get(1)).setSelected(i == 1);
            }
            return null;
        });
        clickIn("#contentArea", "Lưu câu hỏi");
        Thread.sleep(2500);
        System.out.println("  sau khi lưu: " + text("#countLabel") + ", biểu mẫu còn mở: " + find("#contentArea").isPresent());

        clickButton("Đề thi");
        waitFor("#table");
        Thread.sleep(1200);
        shot("gv-3-exams");
        clickButton("Tạo đề mới");
        waitFor("#matrixGrid");
        Thread.sleep(1200);
        onFx(() -> {
            javafx.scene.control.TabPane tabs = (javafx.scene.control.TabPane) mainStage().getScene().getRoot().lookup("#tabs");
            tabs.getSelectionModel().select(1);
            return null;
        });
        Thread.sleep(500);
        clickButton("Kiểm tra ma trận");
        Thread.sleep(500);
        shot("gv-4-matrix");

        clickButton("Ca thi");
        waitFor("#table");
        Thread.sleep(2500);
        shot("gv-5-sessions");
        onFx(() -> {
            @SuppressWarnings("unchecked")
            javafx.scene.control.TableView<com.group2.oes.model.SessionSummary> table =
                    (javafx.scene.control.TableView<com.group2.oes.model.SessionSummary>) mainStage().getScene().getRoot().lookup("#table");
            table.getItems().stream().filter(s -> s.session().getSessionId() == resultSession).findFirst()
                    .ifPresent(s -> table.getSelectionModel().select(s));
            return null;
        });
        clickButton("Xem kết quả");
        waitFor("#resultTable");
        Thread.sleep(2500);
        System.out.println("  kết quả ca " + resultSession + ": " + text("#summaryLabel"));
        System.out.println("  số dòng bảng điểm: " + onFx(() ->
                ((javafx.scene.control.TableView<?>) mainStage().getScene().getRoot().lookup("#resultTable")).getItems().size()));
        shot("gv-6-results");
    }

    private static void admin() throws Exception {
        waitFor("#usernameField");
        login("ui_admin", "admin123");
        waitFor("#keywordField");
        Thread.sleep(1500);
        System.out.println("  số người dùng: " + onFx(() ->
                ((javafx.scene.control.TableView<?>) mainStage().getScene().getRoot().lookup("#table")).getItems().size()));
        shot("admin-1-users");
        clickButton("Môn học & lớp");
        waitFor("#subjectTable");
        Thread.sleep(1500);
        shot("admin-2-subjects");
        clickButton("Nhật ký");
        waitFor("#lines");
        Thread.sleep(1000);
        System.out.println("  số dòng nhật ký: " + onFx(() ->
                ((javafx.scene.control.ListView<?>) mainStage().getScene().getRoot().lookup("#lines")).getItems().size()));
        shot("admin-3-log");
    }

    /** Cửa sổ (chính hoặc cửa sổ con) có chứa nút có fx:id cho trước. */
    private static Stage stageWith(String selector) {
        return (Stage) Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.getScene() != null && s.getScene().getRoot().lookup(selector) != null)
                .findFirst().orElse(null);
    }

    private static <T> T onStage(String selector, java.util.function.Function<Parent, T> work) throws Exception {
        return onFx(() -> work.apply(stageWith(selector).getScene().getRoot()));
    }

    private static void waitForStage(String selector) throws Exception {
        for (int i = 0; i < 100 && onFx(() -> stageWith(selector)) == null; i++) {
            Thread.sleep(100);
        }
        Thread.sleep(500);
    }

    private static void clickIn(String selector, String label) throws Exception {
        Platform.runLater(() -> buttonIn(stageWith(selector).getScene().getRoot(), label).fire());
    }

    /** Cửa sổ con không phủ hết màn hình: chụp cả màn hình là thấy cả cửa sổ chính phía sau. */
    private static void shotStage(String selector, String name) {
        shot(name);
    }

    /** Chọn đáp án đầu tiên (hoặc gõ chữ) cho câu đang hiện. */
    private static void answerCurrent() throws Exception {
        onFx(() -> {
            Parent root = mainStage().getScene().getRoot();
            Node box = root.lookup("#answerBox");
            Node first = ((Parent) box).getChildrenUnmodifiable().stream()
                    .filter(n -> n instanceof javafx.scene.control.ButtonBase || n instanceof TextInputControl)
                    .findFirst().orElseThrow();
            if (first instanceof javafx.scene.control.RadioButton r) {
                r.setSelected(true);
            } else if (first instanceof javafx.scene.control.CheckBox c) {
                c.setSelected(true);
            } else if (first instanceof TextInputControl t) {
                t.setText("extends");
            }
            return null;
        });
    }

    private static long answeredOnGrid() throws Exception {
        return onFx(() -> ((Parent) mainStage().getScene().getRoot().lookup("#questionGrid")).getChildrenUnmodifiable()
                .stream().filter(n -> n.getStyleClass().contains("q-answered")).count());
    }

    /** Không có trình quản lý cửa sổ trong Xvfb nên không Alt+Tab được: thoát toàn màn hình, như khi bấm phím tắt. */
    private static void leaveWindow() throws Exception {
        onFx(() -> {
            mainStage().setFullScreen(false);
            return null;
        });
    }

    private static void deadDb() throws Exception {
        waitForDialog();
        System.out.println("  mất kết nối: " + dialogText());
        shot("deaddb-1-error");
        clickDialogButton("Đóng");
        waitFor("#hostField");
        shot("deaddb-2-server-setup");
    }

    // ------------------------------------------------------------ thao tác

    /** Application.launch chạy trên luồng khác; chờ tới khi JavaFX nhận lệnh runLater. */
    private static void awaitToolkit() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            try {
                Platform.runLater(() -> { });
                return;
            } catch (IllegalStateException notStartedYet) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("JavaFX không khởi động được");
    }

    private static void login(String user, String password) throws Exception {
        type("#usernameField", user);
        type("#passwordField", password);
        clickButton("Đăng nhập");
    }

    private static Stage mainStage() {
        return (Stage) Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.getScene() != null && !(s.getScene().getRoot() instanceof DialogPane))
                .findFirst().orElse(null);
    }

    private static <T> T onFx(Supplier<T> work) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(work.get());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("luồng giao diện không trả lời");
        }
        if (error.get() != null) {
            throw new IllegalStateException(error.get());
        }
        return result.get();
    }

    private static Optional<Node> find(String selector) throws Exception {
        return onFx(() -> {
            Stage s = mainStage();
            return s == null ? Optional.<Node>empty() : Optional.ofNullable(s.getScene().getRoot().lookup(selector));
        });
    }

    private static void waitFor(String selector) throws Exception {
        for (int i = 0; i < 200; i++) {
            if (find(selector).isPresent()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("không thấy " + selector);
    }

    private static void type(String selector, String text) throws Exception {
        onFx(() -> {
            ((TextInputControl) mainStage().getScene().getRoot().lookup(selector)).setText(text);
            return null;
        });
    }

    private static String text(String selector) throws Exception {
        return onFx(() -> ((Labeled) mainStage().getScene().getRoot().lookup(selector)).getText());
    }

    private static void clickButton(String label) throws Exception {
        Platform.runLater(() -> buttonIn(mainStage().getScene().getRoot(), label).fire());
    }

    private static ButtonBase buttonIn(Parent root, String label) {
        return java.util.stream.Stream.concat(root.lookupAll(".button").stream(), root.lookupAll(".toggle-button").stream())
                .filter(n -> n instanceof ButtonBase b && label.equals(b.getText()))
                .map(n -> (ButtonBase) n)
                .findFirst().orElseThrow(() -> new IllegalStateException("không có nút " + label));
    }

    private static DialogPane dialog() throws Exception {
        return onFx(() -> Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.getScene() != null && s.getScene().getRoot() instanceof DialogPane)
                .map(w -> (DialogPane) ((Stage) w).getScene().getRoot())
                .findFirst().orElse(null));
    }

    private static void waitForDialog() throws Exception {
        for (int i = 0; i < 200; i++) {
            if (dialog() != null) {
                Thread.sleep(300);
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("không thấy hộp thoại");
    }

    private static String dialogText() throws Exception {
        DialogPane d = dialog();
        return onFx(() -> ((Labeled) d.getContent()).getText());
    }

    private static void clickDialogButton(String label) throws Exception {
        DialogPane d = dialog();
        Platform.runLater(() -> buttonIn(d, label).fire());
        Thread.sleep(400);
    }

    private static void shot(String name) {
        try {
            Thread.sleep(400);
            new ProcessBuilder("import", "-window", "root", shots.resolve(name + ".png").toString())
                    .inheritIO().start().waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("không chụp được màn hình: " + e);
        }
    }
}
