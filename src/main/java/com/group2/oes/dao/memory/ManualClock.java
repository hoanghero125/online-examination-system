package com.group2.oes.dao.memory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/** Đồng hồ điều khiển bằng tay, để kiểm thử các tình huống "hết giờ" mà không phải chờ thật. */
public class ManualClock implements Supplier<LocalDateTime> {

    private LocalDateTime now;

    public ManualClock(LocalDateTime start) {
        this.now = start;
    }

    @Override
    public synchronized LocalDateTime get() {
        return now;
    }

    public synchronized void advance(Duration d) {
        now = now.plus(d);
    }

    public synchronized void set(LocalDateTime t) {
        now = t;
    }
}
