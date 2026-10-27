package com.group2.oes.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Cách hiển thị ngày giờ và điểm, dùng chung cho mọi màn hình. */
public final class Formats {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Formats() {
    }

    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }

    public static String time(LocalDateTime t) {
        return t == null ? "" : t.format(TIME);
    }

    /** Điểm: bỏ phần thập phân thừa, "7" thay vì "7.00", "6.5" thay vì "6.50". */
    public static String score(double d) {
        String s = String.format(Locale.ROOT, "%.2f", d);
        return s.replaceAll("\\.?0+$", "");
    }
}
