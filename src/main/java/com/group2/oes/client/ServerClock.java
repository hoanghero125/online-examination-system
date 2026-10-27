package com.group2.oes.client;

import java.time.LocalDateTime;
import java.util.function.LongSupplier;

/**
 * Giờ máy chủ, tính trên máy thí sinh mà không dùng đồng hồ của máy thí sinh (quyết định số 6).
 *
 * Lấy giờ máy chủ một lần, ghi kèm System.nanoTime() lúc đó; về sau giờ máy chủ = giờ đã lấy + thời gian đã trôi.
 * nanoTime chỉ đo khoảng thời gian, không phụ thuộc giờ hệ điều hành, nên thí sinh chỉnh đồng hồ máy trạm
 * cũng không làm đồng hồ đếm ngược chạy sai. Màn hình làm bài đồng bộ lại mỗi 60 giây.
 */
public final class ServerClock {

    private final LongSupplier nanoTime;
    private LocalDateTime serverBase;
    private long nanoBase;

    public ServerClock(LocalDateTime serverNow) {
        this(serverNow, System::nanoTime);
    }

    /** Cho kiểm thử: truyền nguồn nanoTime giả. */
    public ServerClock(LocalDateTime serverNow, LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
        resync(serverNow);
    }

    public synchronized LocalDateTime now() {
        return serverBase.plusNanos(nanoTime.getAsLong() - nanoBase);
    }

    public synchronized void resync(LocalDateTime serverNow) {
        this.serverBase = serverNow;
        this.nanoBase = nanoTime.getAsLong();
    }
}
