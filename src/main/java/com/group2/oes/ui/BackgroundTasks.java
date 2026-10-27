package com.group2.oes.ui;

import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Chạy việc gọi service/DAO trên luồng nền.
 *
 * Luồng giao diện của JavaFX (JavaFX Application Thread) mà bị chặn chờ CSDL thì cửa sổ đứng hình,
 * không vẽ lại, không bấm được. Mọi lệnh gọi xuống service đều đi qua lớp này. Kết quả và lỗi
 * được trả về trên luồng giao diện (Task tự làm việc này), nên onSuccess/onError cập nhật giao diện thoải mái.
 */
public final class BackgroundTasks {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    // Vài luồng là đủ: mỗi máy chỉ có một người dùng. Luồng daemon để không giữ chương trình lại khi đóng cửa sổ.
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "oes-bg-" + COUNTER.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private BackgroundTasks() {
    }

    /** Dạng đầy đủ: nơi gọi tự xử lý lỗi. */
    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        // Task giữ ngoại lệ gốc ở getException(), không bọc thêm lớp nào
        task.setOnFailed(e -> onError.accept(task.getException()));
        POOL.execute(task);
    }

    /**
     * Dạng thường dùng: trong lúc chạy thì vô hiệu hóa {@code busy} (nút vừa bấm, hoặc cả khung nhập)
     * và hiện con trỏ chờ; lỗi được hiện bằng ErrorDialogs. Dù thành công hay lỗi, nút luôn được bật lại.
     */
    public static <T> void run(Node busy, Supplier<T> work, Consumer<T> onSuccess) {
        Scene scene = busy.getScene();
        busy.setDisable(true);
        if (scene != null) {
            scene.setCursor(Cursor.WAIT);
        }
        Runnable restore = () -> {
            busy.setDisable(false);
            if (scene != null) {
                scene.setCursor(Cursor.DEFAULT);
            }
        };
        run(work, result -> {
            restore.run();
            onSuccess.accept(result);
        }, error -> {
            restore.run();
            ErrorDialogs.show(error);
        });
    }

    /** Như trên, cho việc không có kết quả trả về. */
    public static void runVoid(Node busy, Runnable work, Runnable onSuccess) {
        run(busy, () -> {
            work.run();
            return Boolean.TRUE;
        }, ignored -> onSuccess.run());
    }

    /** Gọi khi đóng ứng dụng. */
    public static void shutdown() {
        POOL.shutdownNow();
    }
}
