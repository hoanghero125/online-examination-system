package com.group2.oes.ui;

import com.group2.oes.exception.SessionRuleException;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Duration;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Hàng đợi lưu câu trả lời của màn hình làm bài (FR-24, NFR-06).
 *
 * - Đổi câu trả lời: chờ 500 ms rồi mới lưu, để gõ chữ ở câu điền khuyết không sinh hàng chục lượt ghi.
 * - Mỗi câu chỉ giữ giá trị mới nhất (Map theo số câu): đổi ý ba lần lúc mất mạng thì khi có mạng chỉ gửi lần cuối.
 * - Mất kết nối: bật cờ offline (màn hình hiện dải vàng), 3 giây sau thử lại; thí sinh vẫn làm bài bình thường.
 * - CSDL từ chối vì bài đã khóa (hết giờ, đã nộp ở máy khác): dừng hẳn, báo cho màn hình; không thử lại mãi.
 *
 * Các Map chỉ được đụng tới trong khối synchronized vì việc gửi chạy trên luồng nền,
 * còn việc thêm vào hàng đợi chạy trên luồng giao diện.
 */
public class AnswerSaver {

    /** Nơi thật sự ghi xuống CSDL (StudentExamService). Gọi trên luồng nền. */
    public interface Sink {
        void saveAnswer(int order, String response);

        void saveFlag(int order, boolean flagged);
    }

    private static final Duration DEBOUNCE = Duration.millis(500);
    private static final Duration RETRY = Duration.seconds(3);

    private final Sink sink;
    private final Consumer<SessionRuleException> onLocked;
    private final Map<Integer, String> answers = new LinkedHashMap<>();
    private final Map<Integer, Boolean> flags = new LinkedHashMap<>();
    private final PauseTransition debounce = new PauseTransition(DEBOUNCE);
    private final PauseTransition retry = new PauseTransition(RETRY);
    private final SimpleBooleanProperty offline = new SimpleBooleanProperty(false);

    private boolean sending;   // chỉ dùng trên luồng giao diện
    private boolean stopped;

    public AnswerSaver(Sink sink, Consumer<SessionRuleException> onLocked) {
        this.sink = sink;
        this.onLocked = onLocked;
        debounce.setOnFinished(e -> flush());
        retry.setOnFinished(e -> flush());
    }

    public void answerChanged(int order, String response) {
        synchronized (this) {
            answers.put(order, response);
        }
        debounce.playFromStart();
    }

    /** Đánh dấu hiếm khi đổi, nên gửi ngay. */
    public void flagChanged(int order, boolean flagged) {
        synchronized (this) {
            flags.put(order, flagged);
        }
        flush();
    }

    /** Gửi những gì đang chờ trên luồng nền; chỉ một lượt gửi chạy tại một thời điểm. */
    public void flush() {
        if (sending || stopped || !hasPending()) {
            return;
        }
        sending = true;
        BackgroundTasks.run(() -> {
            drain();
            return Boolean.TRUE;
        }, ok -> {
            sending = false;
            offline.set(false);
            flush();   // có thể đã có thay đổi mới trong lúc gửi
        }, error -> {
            sending = false;
            if (error instanceof SessionRuleException locked) {
                stop();
                onLocked.accept(locked);
            } else {
                offline.set(true);
                retry.playFromStart();
            }
        });
    }

    /**
     * Gửi hết hàng đợi, chạy trên luồng nền. Nút "Nộp bài" gọi hàm này trước khi nộp.
     * Mỗi câu chỉ bị xóa khỏi hàng đợi nếu trong lúc gửi thí sinh không đổi ý lần nữa.
     */
    public void drain() {
        while (true) {
            Map.Entry<Integer, String> a = first(answers);
            if (a != null) {
                sink.saveAnswer(a.getKey(), a.getValue());
                removeIfUnchanged(answers, a);
                continue;
            }
            Map.Entry<Integer, Boolean> f = first(flags);
            if (f != null) {
                sink.saveFlag(f.getKey(), f.getValue());
                removeIfUnchanged(flags, f);
                continue;
            }
            return;
        }
    }

    private synchronized <V> Map.Entry<Integer, V> first(Map<Integer, V> map) {
        Iterator<Map.Entry<Integer, V>> it = map.entrySet().iterator();
        if (!it.hasNext()) {
            return null;
        }
        Map.Entry<Integer, V> e = it.next();
        // chép ra (giá trị có thể null: thí sinh xóa câu trả lời), không giữ tham chiếu vào Map đang được sửa
        return new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue());
    }

    private synchronized <V> void removeIfUnchanged(Map<Integer, V> map, Map.Entry<Integer, V> sent) {
        if (map.containsKey(sent.getKey()) && Objects.equals(map.get(sent.getKey()), sent.getValue())) {
            map.remove(sent.getKey());
        }
    }

    public synchronized boolean hasPending() {
        return !answers.isEmpty() || !flags.isEmpty();
    }

    public ReadOnlyBooleanProperty offlineProperty() {
        return offline;
    }

    /** Ngừng tự gửi (đã nộp bài hoặc bài đã khóa). drain() vẫn gọi được. */
    public void stop() {
        stopped = true;
        debounce.stop();
        retry.stop();
    }
}
