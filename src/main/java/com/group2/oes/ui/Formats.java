package com.group2.oes.ui;

import com.group2.oes.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Cách hiển thị ngày giờ và điểm, dùng chung cho mọi màn hình. */
public final class Formats {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("H:mm");

    private Formats() {
    }

    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }

    public static String time(LocalDateTime t) {
        return t == null ? "" : t.format(TIME);
    }

    /**
     * Ghép ngày (DatePicker) và giờ dạng "HH:mm" (ô nhập) thành thời điểm. Nhập sai thì báo rõ ô nào sai.
     * @param label tên ô trên màn hình, ví dụ "Giờ mở"
     */
    public static LocalDateTime combine(LocalDate date, String hhmm, String label) {
        ValidationException.require(date != null, "DATE_EMPTY", "Hãy chọn ngày cho ô \"" + label + "\".");
        try {
            return LocalDateTime.of(date, LocalTime.parse(hhmm.strip(), HH_MM));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ValidationException("TIME_INVALID", "Ô \"" + label + "\" cần giờ dạng HH:mm, ví dụ 07:30.");
        }
    }

    /** Điểm: bỏ phần thập phân thừa, "7" thay vì "7.00", "6.5" thay vì "6.50". */
    public static String score(double d) {
        String s = String.format(Locale.ROOT, "%.2f", d);
        return s.replaceAll("\\.?0+$", "");
    }
}
