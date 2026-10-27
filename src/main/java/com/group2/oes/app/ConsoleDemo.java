package com.group2.oes.app;

import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Criteria;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.ResultStatistics;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.Submission;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.TrueFalseQuestion;
import com.group2.oes.service.ProctoringService;
import com.group2.oes.service.QuestionBankService;
import com.group2.oes.util.PasswordHasher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

/**
 * Chạy thử toàn bộ luồng nghiệp vụ chính trên console, chưa cần giao diện JavaFX.
 *
 *   java ... ConsoleDemo               dùng dữ liệu trong bộ nhớ
 *   java ... ConsoleDemo db.properties dùng MySQL thật (đã chạy schema.sql)
 */
public final class ConsoleDemo {

    private ConsoleDemo() {
    }

    public static void main(String[] args) {
        configureLogging();
        AppContext app;
        String suffix = "";
        if (args.length > 0) {
            app = AppContext.jdbc(ConnectionManager.fromFile(Path.of(args[0])));
            // thêm hậu tố để chạy nhiều lần trên cùng CSDL không bị trùng tên đăng nhập, mã môn
            suffix = "_" + (System.currentTimeMillis() % 100000);
            System.out.println("Chế độ: MySQL (" + args[0] + ")");
        } else {
            app = AppContext.inMemory(LocalDateTime::now);
            System.out.println("Chế độ: dữ liệu trong bộ nhớ");
        }

        try {
            run(app, suffix);
        } catch (DataAccessException e) {
            // Lỗi kỹ thuật: người dùng thấy câu chung chung, chi tiết in ra để nhóm gỡ lỗi
            System.out.println("[LỖI] " + e.getUserMessage());
            e.printStackTrace();
        }
    }

    private static void run(AppContext app, String sfx) {
        step("1. Khởi tạo tài khoản, môn học");
        Administrator admin = app.users.insert(
                new Administrator(0, "admin" + sfx, PasswordHasher.hash("admin123"), "Quản trị", null, true));
        Subject oop = app.subjects.insert("INT1332" + sfx, "Lập trình hướng đối tượng");

        Instructor gv = app.auth.createUser(admin,
                new Instructor(0, "gv.viet" + sfx, PasswordHasher.hash("gv123"), "Phạm Hoàng Việt", null, true));
        app.subjects.assignInstructor(gv, oop);

        Student hoang = newStudent(app, admin, "B24DCCE098" + sfx, "Đỗ Phạm Bảo Hoàng");
        Student anh = newStudent(app, admin, "B24DCCE007" + sfx, "Đỗ Thị Vân Anh");
        Student trung = newStudent(app, admin, "B24DCCE273" + sfx, "Nguyễn Hoàng Trung");
        Student outsider = newStudent(app, admin, "B24DCCE999" + sfx, "Thí sinh không có tên");
        System.out.println("   Đã tạo 1 quản trị viên, 1 giảng viên, 4 thí sinh.");

        attempt("Đăng nhập sai mật khẩu", () -> app.auth.login("gv.viet" + sfx, "sai-mat-khau"));
        User teacher = app.auth.login("gv.viet" + sfx, "gv123");
        System.out.println("   Giảng viên đăng nhập thành công: " + teacher.getFullName());

        step("2. Nhập ngân hàng câu hỏi từ tệp");
        QuestionBankService.ImportResult imported = importSample(app, teacher, oop);
        System.out.println("   Nhập được " + imported.imported() + " câu, " + imported.errors().size() + " dòng lỗi:");
        imported.errors().forEach(err -> System.out.println("     - " + err));

        QuestionBank bank = app.bankService.loadBank(teacher, oop);
        attempt("Thí sinh xem ngân hàng câu hỏi", () -> app.bankService.loadBank(hoang, oop));

        step("3. Sinh đề theo ma trận");
        ExamMatrix tooBig = new ExamMatrix().add(3, Difficulty.HARD, 10);
        attempt("Ma trận đòi 10 câu khó chương 3", () ->
                app.examService.generate(teacher, bank, "Đề quá sức", MultiChoicePolicy.PARTIAL, tooBig, 1, new Random(1)));

        ExamMatrix matrix = new ExamMatrix()
                .add(1, Difficulty.EASY, 4)
                .add(2, Difficulty.MEDIUM, 3)
                .add(3, Difficulty.HARD, 3);
        Exam exam = app.examService.generate(teacher, bank, "Kiểm tra giữa kỳ OOP", MultiChoicePolicy.PARTIAL,
                matrix, 1.0, new Random(2026));
        System.out.printf("   Đề #%d: %d câu, tổng %.1f điểm, chấm câu nhiều đáp án: %s%n",
                exam.getExamId(), exam.getItems().size(), exam.getTotalPoint(), exam.getMultiChoicePolicy());

        step("4. Mở ca thi");
        LocalDateTime now = app.sessions.serverTime();
        ExamSession session = app.sessionService.createSession(teacher, exam, now.minusMinutes(1), now.plusMinutes(60),
                true, true, true, List.of(hoang, anh, trung));
        System.out.println("   Ca #" + session.getSessionId() + " mở tới " + session.getCloseTime().toLocalTime().withNano(0)
                + ", 3 thí sinh, trộn câu và trộn phương án. Đề đã khóa: " + exam.isLocked());
        attempt("Sửa đề sau khi mở ca", () -> exam.addItem(bank.findBy(Criteria.any()).get(0), 1));

        step("5. Thí sinh Hoàng làm đúng toàn bộ rồi nộp");
        Submission s1 = app.submissionService.start(hoang, session.getSessionId());
        List<ExamItem> paper = session.buildPaperFor(hoang);
        System.out.println("   Thứ tự câu Hoàng nhận được: "
                + paper.stream().map(i -> String.valueOf(i.getOrder())).collect(Collectors.joining(", ")));
        for (ExamItem item : paper) {
            app.submissionService.saveAnswer(hoang, session.getSessionId(), item.getOrder(), correctResponse(item.getQuestion()));
        }
        s1 = app.submissionService.submit(hoang, session.getSessionId(), EndReason.MANUAL);
        System.out.printf("   Hoàng nộp bài: %.2f/%.1f điểm%n", s1.getTotalScore(), exam.getTotalPoint());
        attempt("Hoàng vào thi lần hai", () -> app.submissionService.start(hoang, session.getSessionId()));
        Submission again = app.submissionService.submit(hoang, session.getSessionId(), EndReason.MANUAL);
        System.out.printf("   Hoàng bấm nộp lần nữa: không lỗi, điểm giữ nguyên %.2f%n", again.getTotalScore());

        step("6. Thí sinh Vân Anh làm một nửa rồi rời cửa sổ nhiều lần");
        app.submissionService.start(anh, session.getSessionId());
        List<ExamItem> paperAnh = session.buildPaperFor(anh);
        for (int i = 0; i < paperAnh.size() / 2; i++) {
            ExamItem it = paperAnh.get(i);
            app.submissionService.saveAnswer(anh, session.getSessionId(), it.getOrder(), correctResponse(it.getQuestion()));
        }
        Instant t = Instant.now();
        // lần 2 cách lần 1 có 1 giây: là cùng một thao tác Alt+Tab, phải được gộp
        long[] offsets = {0, 1, 10, 20, 30, 40, 50};
        for (long off : offsets) {
            ProctoringService.ViolationResult r =
                    app.proctoring.report(anh, session.getSessionId(), t.plusSeconds(off));
            System.out.printf("   +%2ds: %s%n", off,
                    r.merged() ? "gộp với lần trước, vẫn " + r.count() + " lần"
                            : r.terminated() ? "vi phạm lần " + r.count() + " -> KẾT THÚC BÀI THI"
                            : "vi phạm lần " + r.count() + ", còn " + r.remaining() + " lần");
            if (r.terminated()) {
                break;
            }
        }
        Submission s2 = app.submissionService.resultOf(anh, session.getSessionId());
        System.out.printf("   Bài của Vân Anh: %.2f điểm, kết thúc do vi phạm: %s%n",
                s2.getTotalScore(), s2.isTerminatedByViolation());
        attempt("Vân Anh cố trả lời tiếp", () ->
                app.submissionService.saveAnswer(anh, session.getSessionId(), 1, "x"));

        step("7. Các trường hợp bị từ chối");
        attempt("Thí sinh không có tên trong ca thi", () -> app.submissionService.start(outsider, session.getSessionId()));
        attempt("Giảng viên gọi chức năng vào thi", () -> app.submissionService.start(teacher, session.getSessionId()));
        attempt("Trung ghi đáp án khi chưa vào thi", () ->
                app.submissionService.saveAnswer(trung, session.getSessionId(), 1, "1"));

        step("8. Trung vào thi, làm đúng 3 câu đầu rồi nộp");
        app.submissionService.start(trung, session.getSessionId());
        for (ExamItem it : exam.getItems().subList(0, 3)) {
            app.submissionService.saveAnswer(trung, session.getSessionId(), it.getOrder(), correctResponse(it.getQuestion()));
        }
        Submission s3 = app.submissionService.submit(trung, session.getSessionId(), EndReason.MANUAL);
        System.out.printf("   Trung: %.2f điểm, trả lời %d/%d câu%n", s3.getTotalScore(), s3.answeredCount(),
                exam.getItems().size());

        step("9. Thống kê ca thi");
        ResultStatistics st = app.statistics.statsFor(teacher, session);
        System.out.printf("   %d bài | cao nhất %.2f | thấp nhất %.2f | trung bình %.2f | kết thúc do vi phạm: %d%n",
                st.count(), st.max(), st.min(), st.average(), st.terminatedByViolation());
        System.out.println("   Phổ điểm (hệ 10): " + st.getHistogram());
        System.out.println("   3 câu khó nhất: " + st.hardestItems(3).stream()
                .map(e -> "câu " + e.getKey() + " (" + Math.round(e.getValue() * 100) + "%)")
                .collect(Collectors.joining(", ")));
        try {
            Path csv = app.statistics.exportCsv(teacher, session, Files.createTempFile("bang-diem-", ".csv"));
            System.out.println("   Đã xuất bảng điểm: " + csv);
            Files.readAllLines(csv).forEach(l -> System.out.println("     " + l.replace("﻿", "")));
        } catch (IOException e) {
            System.out.println("   [LỖI] Không ghi được tệp bảng điểm: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ tiện ích

    private static Student newStudent(AppContext app, Administrator admin, String code, String name) {
        return app.auth.createUser(admin,
                new Student(0, code.toLowerCase(), PasswordHasher.hash("sv123"), name, null, true, code));
    }

    private static QuestionBankService.ImportResult importSample(AppContext app, User teacher, Subject s) {
        try (InputStream in = ConsoleDemo.class.getResourceAsStream("/sample-questions.txt")) {
            if (in == null) {
                throw new IOException("Không tìm thấy sample-questions.txt trong classpath");
            }
            Path tmp = Files.createTempFile("cau-hoi-", ".txt");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return app.bankService.importFile(teacher, s, tmp);
        } catch (IOException e) {
            throw new IllegalStateException("Không chuẩn bị được tệp câu hỏi mẫu", e);
        }
    }

    /** Sinh câu trả lời đúng cho một câu hỏi, chỉ dùng trong chương trình chạy thử. */
    static String correctResponse(Question q) {
        return switch (q.getType()) {
            case SINGLE_CHOICE, MULTI_CHOICE -> q.getOptions().stream()
                    .filter(AnswerOption::isCorrect)
                    .map(o -> String.valueOf(o.getOptionId()))
                    .collect(Collectors.joining(","));
            case TRUE_FALSE -> String.valueOf(((TrueFalseQuestion) q).getCorrectValue());
            case FILL_IN_BLANK -> ((FillInBlankQuestion) q).getAccepted().get(0).toUpperCase();
        };
    }

    /** Đọc logging.properties trong classpath để nhật ký ghi ra tệp thay vì lẫn vào màn hình. */
    private static void configureLogging() {
        try (InputStream in = ConsoleDemo.class.getResourceAsStream("/logging.properties")) {
            if (in != null) {
                LogManager.getLogManager().readConfiguration(in);
            }
        } catch (IOException e) {
            System.err.println("Không đọc được cấu hình nhật ký, dùng mặc định: " + e.getMessage());
        }
    }

    private static void step(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    /**
     * Chạy một thao tác dự kiến bị từ chối và in thông báo người dùng sẽ thấy.
     * Màn hình JavaFX sau này bắt ngoại lệ đúng theo cách này: một nhánh catch cho ExamAppException.
     */
    private static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println("   [?] " + label + ": không bị chặn");
        } catch (ExamAppException e) {
            System.out.println("   [chặn] " + label + " -> " + e.getUserMessage() + "  (" + e.getCode() + ")");
        }
    }
}
