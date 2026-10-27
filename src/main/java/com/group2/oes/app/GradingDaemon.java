package com.group2.oes.app;

import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.exception.ExamAppException;
import com.group2.oes.service.GradingScheduler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

/**
 * Máy chấm chạy không giao diện trên máy chủ CSDL (FR-26, FR-31).
 * Cần khi không có máy giảng viên nào mở StaffApp trong giờ thi: bài nộp vẫn được chấm ngay.
 *
 * <pre>
 *   java ... com.group2.oes.app.GradingDaemon [db-staff.properties]
 *   ./mvnw compile exec:java -Dexec.mainClass=com.group2.oes.app.GradingDaemon -Dexec.args=db-staff.properties
 * </pre>
 * Dừng bằng Ctrl+C.
 */
public final class GradingDaemon {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private GradingDaemon() {
    }

    public static void main(String[] args) throws InterruptedException {
        LoggingSetup.configure();
        Path config = Path.of(args.length > 0 ? args[0] : "db-staff.properties");
        if (!Files.exists(config)) {
            System.err.println("Không tìm thấy tệp cấu hình " + config.toAbsolutePath()
                    + ". Sao chép config/db-staff.properties.example thành db-staff.properties rồi sửa địa chỉ máy chủ.");
            System.exit(1);
        }

        AppContext app;
        try {
            app = AppContext.jdbc(ConnectionManager.fromFile(config));
            app.sessions.serverTime();   // thử kết nối ngay để báo lỗi sớm
        } catch (ExamAppException e) {
            System.err.println(e.getUserMessage() + " (" + e.getMessage() + ")");
            System.exit(1);
            return;
        }

        GradingScheduler scheduler = new GradingScheduler(app.gradingWorker, GradingScheduler.DEFAULT_PERIOD,
                r -> System.out.printf("%s  nộp do hết giờ: %d, đã chấm: %d bài%s%n",
                        LocalTime.now().format(TIME), r.closed(), r.graded(),
                        r.failed() > 0 ? ", lỗi: " + r.failed() + " (xem oes-audit.log)" : ""));
        CountDownLatch stopped = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.close();
            System.out.println("Đã dừng máy chấm.");
            stopped.countDown();
        }));

        scheduler.start();
        System.out.println("Máy chấm đang chạy (" + config.toAbsolutePath() + "), "
                + GradingScheduler.DEFAULT_PERIOD.toSeconds() + " giây một lượt. Nhấn Ctrl+C để dừng.");
        stopped.await();
    }
}
