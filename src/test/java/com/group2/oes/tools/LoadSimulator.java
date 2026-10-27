package com.group2.oes.tools;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.client.PaperItem;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
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
import com.group2.oes.service.GradingWorker;
import com.group2.oes.util.PasswordHasher;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kiểm chứng NFR-01 (Phase 7): 60 máy trạm cùng nối một CSDL, lưu câu trả lời phản hồi dưới 2 giây.
 *
 * Giả lập {@code students} thí sinh, mỗi người: đăng nhập, vào thi, cứ {@code intervalMs} lại lưu một câu trả lời,
 * sau {@code seconds} giây thì nộp bài. Phía thí sinh dùng tài khoản oes_client (đi qua quyền và trigger thật).
 * Cuối cùng máy chấm chấm toàn bộ bài một lượt (NFR-02). In ra p50 / p95 / max thời gian từng thao tác.
 *
 * <pre>
 *   java -cp ... com.group2.oes.tools.LoadSimulator db-staff.properties db-client.properties [60] [180] [2000]
 * </pre>
 */
public final class LoadSimulator {

    private LoadSimulator() {
    }

    public static void main(String[] args) throws Exception {
        AppContext staff = AppContext.jdbc(ConnectionManager.fromFile(Path.of(args[0])));
        ClientContext client = ClientContext.jdbc(ConnectionManager.fromFile(Path.of(args[1])));
        int students = args.length > 2 ? Integer.parseInt(args[2]) : 60;
        int seconds = args.length > 3 ? Integer.parseInt(args[3]) : 180;
        int intervalMs = args.length > 4 ? Integer.parseInt(args[4]) : 2000;

        System.out.printf("Chuẩn bị %d thí sinh, %d giây, lưu bài mỗi %d ms…%n", students, seconds, intervalMs);
        int sessionId = prepare(staff, students, seconds);

        List<Long> enterTimes = Collections.synchronizedList(new ArrayList<>());
        List<Long> saveTimes = Collections.synchronizedList(new ArrayList<>());
        List<Long> submitTimes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failures = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(students);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);

        for (int i = 1; i <= students; i++) {
            String username = String.format("load_sv%03d", i);
            int seed = i;
            pool.submit(() -> {
                Random random = new Random(seed);
                try {
                    // các máy không bấm "Vào thi" cùng một mili giây: rải đều trong khoảng lưu bài đầu tiên
                    Thread.sleep(random.nextInt(intervalMs));
                    User user = client.auth.login(username, "load123");
                    long t0 = System.nanoTime();
                    ExamAttempt attempt = client.exams.enter(user, sessionId);
                    enterTimes.add(System.nanoTime() - t0);
                    List<PaperItem> items = attempt.paper().getItems();
                    while (System.nanoTime() < deadline) {
                        PaperItem it = items.get(random.nextInt(items.size()));
                        long t1 = System.nanoTime();
                        try {
                            client.exams.saveAnswer(user, attempt, it.order(), answerFor(it, random));
                            saveTimes.add(System.nanoTime() - t1);
                        } catch (RuntimeException e) {
                            failures.incrementAndGet();
                        }
                        Thread.sleep(intervalMs);
                    }
                    long t2 = System.nanoTime();
                    client.exams.submit(user, attempt, EndReason.MANUAL);
                    submitTimes.add(System.nanoTime() - t2);
                } catch (Exception e) {
                    failures.incrementAndGet();
                    System.out.println("  " + username + ": " + e);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(seconds + 120L, TimeUnit.SECONDS);

        long g0 = System.nanoTime();
        GradingWorker.RunResult graded = staff.gradingWorker.runOnce();
        long gradingMs = (System.nanoTime() - g0) / 1_000_000;

        System.out.println();
        System.out.println("KẾT QUẢ (thời gian tính bằng mili giây)");
        report("Vào thi (enter)", enterTimes);
        report("Lưu câu trả lời (saveAnswer)", saveTimes);
        report("Nộp bài (submit)", submitTimes);
        System.out.printf("Lỗi: %d%n", failures.get());
        System.out.printf("Máy chấm: chấm %d bài trong %d ms (%.0f ms/bài)%n", graded.graded(), gradingMs,
                graded.graded() == 0 ? 0.0 : (double) gradingMs / graded.graded());
        System.exit(0);
    }

    private static String answerFor(PaperItem it, Random random) {
        return switch (it.type()) {
            case SINGLE_CHOICE -> String.valueOf(it.options().get(random.nextInt(it.options().size())).optionId());
            case MULTI_CHOICE -> it.options().get(0).optionId() + "," + it.options().get(1).optionId();
            case TRUE_FALSE -> String.valueOf(random.nextBoolean());
            case FILL_IN_BLANK -> random.nextBoolean() ? "extends" : "implements";
        };
    }

    private static void report(String label, List<Long> nanos) {
        List<Long> sorted = new ArrayList<>(nanos);
        Collections.sort(sorted);
        if (sorted.isEmpty()) {
            System.out.println(label + ": không có số liệu");
            return;
        }
        System.out.printf("%-30s n=%-6d p50=%-6.1f p95=%-6.1f p99=%-6.1f max=%.1f%n", label, sorted.size(),
                ms(percentile(sorted, 50)), ms(percentile(sorted, 95)), ms(percentile(sorted, 99)), ms(sorted.get(sorted.size() - 1)));
    }

    private static long percentile(List<Long> sorted, int p) {
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    /** Môn LOAD, 20 câu đủ 4 loại, {@code students} thí sinh, một ca mở ngay, đóng sau thời gian chạy thử + 5 phút. */
    private static int prepare(AppContext app, int students, int seconds) {
        Administrator admin = (Administrator) app.users.findByUsername("load_admin").orElseGet(() ->
                app.users.insert(new Administrator(0, "load_admin", PasswordHasher.hash("load123"), "Load Admin", null, true)));
        Instructor gv = (Instructor) app.users.findByUsername("load_gv").orElseGet(() ->
                app.auth.createUser(admin, new Instructor(0, "load_gv", PasswordHasher.hash("load123"), "Load GV", null, true)));
        Subject subject = app.subjects.findByCode("LOAD").orElseGet(() -> app.subjects.insert("LOAD", "Kiểm thử tải"));
        app.subjects.assignInstructor(gv, subject);

        List<Student> list = new ArrayList<>();
        for (int i = 1; i <= students; i++) {
            String username = String.format("load_sv%03d", i);
            String code = String.format("LOAD%03d", i);
            list.add((Student) app.users.findByUsername(username).orElseGet(() -> app.users.insert(
                    new Student(0, username, PasswordHasher.hash("load123"), "Thí sinh tải " + code, null, true, code))));
        }

        Map<Integer, Double> picks = new LinkedHashMap<>();
        for (int i = 1; i <= 20; i++) {
            QuestionData base = QuestionData.basic(subject, "Câu tải " + i, 1 + i % 3, Difficulty.values()[i % 3], 1);
            Question q = switch (i % 4) {
                case 0 -> app.bankService.addQuestion(gv, QuestionType.SINGLE_CHOICE, base.withOptions(List.of(
                        AnswerOption.of("A", true), AnswerOption.of("B", false), AnswerOption.of("C", false))));
                case 1 -> app.bankService.addQuestion(gv, QuestionType.MULTI_CHOICE, base.withOptions(List.of(
                        AnswerOption.of("A", true), AnswerOption.of("B", true), AnswerOption.of("C", false))));
                case 2 -> app.bankService.addQuestion(gv, QuestionType.TRUE_FALSE, base.withCorrectValue(true));
                default -> app.bankService.addQuestion(gv, QuestionType.FILL_IN_BLANK, base.withAccepted(List.of("extends"), false));
            };
            picks.put(q.getQuestionId(), 0.5);
        }
        Exam exam = app.examService.createManual(gv, app.bankService.loadBank(gv, subject), "Đề kiểm thử tải",
                MultiChoicePolicy.PARTIAL, picks);
        LocalDateTime now = app.sessions.serverTime();
        ExamSession s = app.sessionService.createSession(gv, exam,
                new SessionWindow(now.minusMinutes(1), now.plusSeconds(seconds).plusMinutes(5)),
                new SessionPolicy(true, true, false), list);
        return s.getSessionId();
    }
}
