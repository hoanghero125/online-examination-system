package com.group2.oes.ui;

import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Giám sát cửa sổ làm bài (FR-27, FR-28): toàn màn hình, không thoát được bằng Esc, không đóng được bằng Alt+F4,
 * và báo mỗi lần thí sinh rời khỏi cửa sổ.
 *
 * Ba sự kiện cùng gọi một hàm onLeave: thoát toàn màn hình, mất tiêu điểm (Alt+Tab), thu nhỏ cửa sổ.
 * Một lần Alt+Tab thường phát cả hai, ba sự kiện; việc gộp chúng làm một do ViolationPolicy quyết định.
 *
 * Chính hộp thoại của ứng dụng (cảnh báo vi phạm, xác nhận nộp bài) cũng làm cửa sổ mất tiêu điểm. Vì vậy
 * mọi hộp thoại trong lúc thi phải mở qua showDialog(): tạm ngừng nghe, và chỉ nghe lại khi cửa sổ đã trở lại
 * toàn màn hình và có tiêu điểm – không phải ngay lúc hộp thoại đóng, vì trên đường quay lại toàn màn hình
 * JavaFX còn phát thêm vài sự kiện nữa.
 */
public final class ExamProctor {

    private static final Duration RESUME_FALLBACK = Duration.seconds(2);

    private final Stage stage;
    private final Consumer<Instant> onLeave;
    private final PauseTransition resumeFallback = new PauseTransition(RESUME_FALLBACK);
    private boolean watching;
    private boolean paused;
    private boolean resumePending;
    private EventHandler<WindowEvent> previousCloseHandler;

    private final ChangeListener<Boolean> fullScreenListener = (obs, was, isFull) -> {
        if (isFull) {
            tryResume();
        } else {
            leave();
        }
    };
    private final ChangeListener<Boolean> focusListener = (obs, was, focused) -> {
        if (focused) {
            tryResume();
        } else {
            leave();
        }
    };
    private final ChangeListener<Boolean> iconifiedListener = (obs, was, iconified) -> {
        if (iconified) {
            leave();
        }
    };

    public ExamProctor(Stage stage, Consumer<Instant> onLeave) {
        this.stage = stage;
        this.onLeave = onLeave;
        resumeFallback.setOnFinished(e -> forceResume());
    }

    public void start() {
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);   // Esc không thoát được
        stage.setFullScreenExitHint("");
        previousCloseHandler = stage.getOnCloseRequest();
        stage.setOnCloseRequest(Event::consume);                          // chặn Alt+F4 trong lúc thi
        stage.setFullScreen(true);
        stage.fullScreenProperty().addListener(fullScreenListener);
        stage.focusedProperty().addListener(focusListener);
        stage.iconifiedProperty().addListener(iconifiedListener);
        watching = true;
    }

    /** Kết thúc bài thi: bỏ nghe, trả lại cửa sổ bình thường. */
    public void stop() {
        watching = false;
        resumeFallback.stop();
        stage.fullScreenProperty().removeListener(fullScreenListener);
        stage.focusedProperty().removeListener(focusListener);
        stage.iconifiedProperty().removeListener(iconifiedListener);
        stage.setOnCloseRequest(previousCloseHandler);
        stage.setFullScreen(false);
    }

    /** Mở hộp thoại của chính ứng dụng (showAndWait bên trong {@code show}) mà không tính là rời cửa sổ. */
    public void showDialog(Runnable show) {
        paused = true;
        try {
            show.run();
        } finally {
            resumePending = true;
            if (watching && !stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
            tryResume();
            resumeFallback.playFromStart();
        }
    }

    /**
     * Lưới an toàn: nếu sau vài giây cửa sổ vẫn chưa có lại tiêu điểm (trình quản lý cửa sổ lạ, thí sinh đã
     * chuyển sang cửa sổ khác ngay khi đóng hộp thoại), không được để việc giám sát tắt mãi.
     * Bật nghe lại, và nếu lúc đó thí sinh đang ở ngoài cửa sổ thì tính là một lần rời cửa sổ.
     */
    private void forceResume() {
        if (!paused || !watching) {
            return;
        }
        resumePending = false;
        paused = false;
        if (!stage.isFullScreen() || !stage.isFocused()) {
            leave();
        }
    }

    private void tryResume() {
        if (resumePending && stage.isFullScreen() && stage.isFocused()) {
            resumePending = false;
            paused = false;
        }
    }

    private void leave() {
        if (watching && !paused) {
            onLeave.accept(Instant.now());
        }
    }

    public boolean isPaused() {
        return paused;
    }
}
