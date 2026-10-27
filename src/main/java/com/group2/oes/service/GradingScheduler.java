package com.group2.oes.service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hẹn giờ cho máy chấm: gọi GradingWorker.runOnce() đều đặn (mặc định 3 giây/lần, FR-26).
 *
 * Tách khỏi GradingWorker (SRP): GradingWorker biết chấm một lượt, lớp này chỉ biết lặp lại.
 * Nhờ vậy kiểm thử gọi thẳng runOnce() mà không phải chờ đồng hồ.
 * Dùng chung cho StaffApp (chạy nền khi giảng viên mở ứng dụng) và GradingDaemon (máy chủ không giao diện).
 */
public class GradingScheduler implements AutoCloseable {

    public static final Duration DEFAULT_PERIOD = Duration.ofSeconds(3);

    private static final Logger LOG = Logger.getLogger(GradingScheduler.class.getName());

    private final GradingWorker worker;
    private final Duration period;
    private final Consumer<GradingWorker.RunResult> onWork;
    private ScheduledExecutorService executor;
    private volatile boolean failing;   // đang mất kết nối: chỉ ghi nhật ký lần đầu và lúc kết nối lại

    /** @param onWork được gọi sau mỗi lượt có nộp hoặc chấm bài (in ra màn hình, làm mới giao diện...) */
    public GradingScheduler(GradingWorker worker, Duration period, Consumer<GradingWorker.RunResult> onWork) {
        this.worker = worker;
        this.period = period;
        this.onWork = onWork;
    }

    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grading-worker");
            t.setDaemon(true);   // không giữ chương trình lại khi người dùng đóng ứng dụng
            return t;
        });
        executor.scheduleWithFixedDelay(this::runNow, 0, period.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Chạy một lượt ngay (lịch định kỳ gọi hàm này; giao diện có thể gọi khi bấm "Chấm ngay").
     * Phải bắt mọi ngoại lệ: ScheduledExecutorService sẽ lặng lẽ dừng hẳn việc lặp lại
     * nếu một lượt ném ngoại lệ ra ngoài.
     */
    public void runNow() {
        try {
            GradingWorker.RunResult r = worker.runOnce();
            if (failing) {
                failing = false;
                LOG.info("Máy chấm đã kết nối lại CSDL.");
            }
            if (r.hasWork()) {
                onWork.accept(r);
            }
        } catch (RuntimeException e) {
            if (!failing) {
                LOG.log(Level.WARNING, "Máy chấm không chạy được lượt này, sẽ thử lại sau " + period.toSeconds() + " giây", e);
            }
            failing = true;
        }
    }

    @Override
    public synchronized void close() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public synchronized boolean isRunning() {
        return executor != null;
    }
}
