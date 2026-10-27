package com.group2.oes.tools;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.LoggingSetup;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.service.ImportResult;
import com.group2.oes.util.PasswordHasher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tạo dữ liệu mẫu cho buổi demo (Phase 8), chạy một lần trên CSDL vừa tạo bằng schema.sql và security.sql:
 * 1 quản trị viên, 2 giảng viên, 2 môn, 2 lớp học phần, 60 thí sinh B24DCCE001…060 và 4 thành viên nhóm,
 * 60 câu hỏi môn OOP (nạp từ seed-questions.txt qua chức năng nhập tệp), 1 đề sinh theo ma trận,
 * 1 ca thi mở ngay trong 2 giờ. Cuối cùng in bảng tài khoản.
 *
 * <pre>
 *   java -cp oes-staff.jar com.group2.oes.tools.SeedTool [db-staff.properties]
 * </pre>
 * Mọi thao tác đi qua service như người dùng thật, nên dữ liệu mẫu cũng kiểm tra luôn nghiệp vụ.
 */
public final class SeedTool {

    static final String ADMIN_PASSWORD = "admin123";
    static final String STAFF_PASSWORD = "gv123456";
    static final String STUDENT_PASSWORD = "sv123456";

    /** Bốn thành viên nhóm 2, lấy đúng như trang bìa các báo cáo. */
    static final Map<String, String> MEMBERS = Map.of(
            "B24DCCE098", "Đỗ Phạm Bảo Hoàng",
            "B24DCCE007", "Đỗ Thị Vân Anh",
            "B24DCCE273", "Nguyễn Hoàng Trung",
            "B24DCCE070", "Phan Tùng Dương");

    private static final String[] FAMILY = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Ngô", "Dương", "Lý"};
    private static final String[] MIDDLE = {"Văn", "Thị", "Minh", "Đức", "Thu", "Quang", "Ngọc", "Hữu", "Thanh", "Gia"};
    private static final String[] GIVEN = {"An", "Bình", "Châu", "Dũng", "Giang", "Hà", "Hải", "Hạnh", "Hùng", "Khánh", "Linh",
            "Long", "Mai", "Nam", "Ngân", "Phong", "Phương", "Quân", "Sơn", "Thảo", "Trang", "Tú", "Tuấn", "Vy", "Yến"};

    private SeedTool() {
    }

    public static void main(String[] args) {
        LoggingSetup.configure();
        Path config = Path.of(args.length > 0 ? args[0] : "db-staff.properties");
        if (!Files.exists(config)) {
            System.err.println("Không tìm thấy " + config.toAbsolutePath()
                    + ". Sao chép config/db-staff.properties.example thành db-staff.properties rồi sửa địa chỉ máy chủ.");
            System.exit(1);
        }
        try {
            AppContext app = AppContext.jdbc(ConnectionManager.fromFile(config));
            if (app.subjects.findByCode("INT1332").isPresent()) {
                System.err.println("CSDL đã có dữ liệu mẫu (môn INT1332). Muốn tạo lại từ đầu: chạy lại db/schema.sql "
                        + "rồi db/security.sql, sau đó chạy SeedTool.");
                System.exit(2);
            }
            seed(app);
        } catch (ExamAppException e) {
            System.err.println(e.getUserMessage() + " (" + e.getMessage() + ")");
            System.exit(1);
        }
    }

    /** Tạo toàn bộ dữ liệu mẫu qua các service (tách khỏi main để kiểm thử được với CSDL bộ nhớ). */
    public static void seed(AppContext app) {
        Administrator admin = app.users.insert(new Administrator(0, "admin", PasswordHasher.hash(ADMIN_PASSWORD),
                "Quản trị hệ thống", "admin@ptit.edu.vn", true));
        Instructor viet = app.auth.createUser(admin, new Instructor(0, "gv.viet", PasswordHasher.hash(STAFF_PASSWORD),
                "Phạm Hoàng Việt", "viet.ph@ptit.edu.vn", true));
        Instructor lan = app.auth.createUser(admin, new Instructor(0, "gv.lan", PasswordHasher.hash(STAFF_PASSWORD),
                "Nguyễn Thị Lan", "lan.nt@ptit.edu.vn", true));

        Subject oop = app.subjectService.createSubject(admin, "INT1332", "Lập trình hướng đối tượng");
        Subject db = app.subjectService.createSubject(admin, "INT1313", "Cơ sở dữ liệu");
        app.subjectService.assignInstructor(admin, viet, oop);
        app.subjectService.assignInstructor(admin, lan, db);
        CourseClass oopClass = app.subjectService.createClass(admin, "INT1332-01", oop, "HK1 2026-2027");
        CourseClass dbClass = app.subjectService.createClass(admin, "INT1313-01", db, "HK1 2026-2027");

        // 60 thí sinh: 001–040 học lớp OOP, 041–060 học lớp CSDL. Mã trùng mã thành viên nhóm thì dùng tên thật.
        List<Student> oopStudents = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            String code = String.format("B24DCCE%03d", i);
            Student s = newStudent(app, admin, code, MEMBERS.getOrDefault(code, generatedName(i)), i <= 40 ? oopClass : dbClass);
            if (i <= 40) {
                oopStudents.add(s);
            }
        }
        for (Map.Entry<String, String> m : MEMBERS.entrySet()) {
            if (Integer.parseInt(m.getKey().substring(7)) > 60) {
                oopStudents.add(newStudent(app, admin, m.getKey(), m.getValue(), oopClass));
            }
        }

        ImportResult imported = importQuestions(app, viet, oop);
        if (!imported.errors().isEmpty()) {
            throw new IllegalStateException("Tệp câu hỏi mẫu có lỗi: " + imported.errors());
        }

        // 20 câu, mỗi câu 0,5 điểm = 10 điểm. Mỗi ô của ma trận đều nhỏ hơn số câu ngân hàng có.
        QuestionBank bank = app.bankService.loadBank(viet, oop);
        ExamMatrix matrix = new ExamMatrix()
                .add(1, Difficulty.EASY, 3).add(1, Difficulty.MEDIUM, 2).add(1, Difficulty.HARD, 2)
                .add(2, Difficulty.EASY, 3).add(2, Difficulty.MEDIUM, 2).add(2, Difficulty.HARD, 2)
                .add(3, Difficulty.EASY, 2).add(3, Difficulty.MEDIUM, 2).add(3, Difficulty.HARD, 2);
        Exam exam = app.examService.generate(viet, bank, "Kiểm tra giữa kỳ OOP", MultiChoicePolicy.PARTIAL, matrix, 0.5,
                new Random(2026));

        LocalDateTime now = app.sessions.serverTime().withSecond(0).withNano(0);
        ExamSession session = app.sessionService.createSession(viet, exam, new SessionWindow(now, now.plusHours(2)),
                new SessionPolicy(true, true, true), oopStudents);

        printSummary(imported, exam, session, oopStudents.size());
    }

    private static Student newStudent(AppContext app, Administrator admin, String code, String name, CourseClass cls) {
        Student s = new Student(0, code.toLowerCase(), PasswordHasher.hash(STUDENT_PASSWORD), name,
                code.toLowerCase() + "@stu.ptit.edu.vn", true, code);
        s.setCourseClass(cls);
        return app.auth.createUser(admin, s);
    }

    /** Họ tên giả, cố định theo số thứ tự để mỗi lần tạo lại đều giống nhau. */
    static String generatedName(int i) {
        return FAMILY[i % FAMILY.length] + " " + MIDDLE[(i * 7) % MIDDLE.length] + " " + GIVEN[(i * 11) % GIVEN.length];
    }

    private static ImportResult importQuestions(AppContext app, Instructor teacher, Subject subject) {
        try (InputStream in = SeedTool.class.getResourceAsStream("/seed-questions.txt")) {
            if (in == null) {
                throw new IllegalStateException("Thiếu tệp seed-questions.txt trong chương trình");
            }
            Path tmp = Files.createTempFile("seed-questions-", ".txt");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            ImportResult r = app.bankService.importFile(teacher, subject, tmp);
            Files.deleteIfExists(tmp);
            return r;
        } catch (IOException e) {
            throw new IllegalStateException("Không chuẩn bị được tệp câu hỏi mẫu", e);
        }
    }

    private static void printSummary(ImportResult imported, Exam exam, ExamSession session, int enrolled) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        System.out.println();
        System.out.println("Đã tạo dữ liệu mẫu:");
        System.out.println("  - " + imported.imported() + " câu hỏi môn INT1332");
        System.out.printf("  - đề \"%s\": %d câu, %.1f điểm (sinh theo ma trận)%n", exam.getTitle(), exam.getItems().size(),
                exam.getTotalPoint());
        System.out.println("  - ca thi #" + session.getSessionId() + " mở từ " + session.getWindow().openTime().format(f)
                + " đến " + session.getWindow().closeTime().format(f) + ", " + enrolled + " thí sinh lớp INT1332-01");
        System.out.println();
        System.out.println("TÀI KHOẢN");
        System.out.println("  Vai trò        Tên đăng nhập     Mật khẩu    Ghi chú");
        System.out.println("  Quản trị viên  admin             " + ADMIN_PASSWORD + "    ");
        System.out.println("  Giảng viên     gv.viet           " + STAFF_PASSWORD + "    phụ trách INT1332");
        System.out.println("  Giảng viên     gv.lan            " + STAFF_PASSWORD + "    phụ trách INT1313");
        System.out.println("  Thí sinh       b24dcce001…060    " + STUDENT_PASSWORD + "    001–040 lớp INT1332-01, 041–060 lớp INT1313-01");
        MEMBERS.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(m -> System.out.printf(
                "  Thành viên     %-17s %s    %s%n", m.getKey().toLowerCase(), STUDENT_PASSWORD, m.getValue()));
    }
}
