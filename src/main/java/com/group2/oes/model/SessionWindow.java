package com.group2.oes.model;

import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.exception.ValidationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Khung giờ của một ca thi: giờ mở và giờ đóng dùng chung cho mọi thí sinh (BR-03).
 *
 * Tách khỏi ExamSession (SRP) để cả hai phía dùng chung một chỗ tính giờ:
 * máy giảng viên (ExamSession) và máy thí sinh (SessionInfo) không được hiểu "đang mở" theo hai cách khác nhau.
 * Mọi hàm cần "bây giờ" đều nhận thời điểm từ ngoài vào, vì giờ chuẩn phải lấy từ máy chủ CSDL.
 */
public record SessionWindow(LocalDateTime openTime, LocalDateTime closeTime) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /** Còn bao lâu thì đồng hồ làm bài đổi màu đỏ và nhắc thí sinh (FR-23). */
    public static final Duration ALMOST_OVER = Duration.ofMinutes(5);

    public SessionWindow {
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(closeTime, "closeTime");
        ValidationException.require(closeTime.isAfter(openTime), "SESSION_TIME",
                "Giờ đóng ca thi phải sau giờ mở.");
    }

    public boolean isUpcoming(LocalDateTime now) {
        return now.isBefore(openTime);
    }

    /** Mở từ đúng giờ mở (tính cả giây đó) tới trước giờ đóng. */
    public boolean isOpen(LocalDateTime now) {
        return !now.isBefore(openTime) && now.isBefore(closeTime);
    }

    public boolean isClosed(LocalDateTime now) {
        return !now.isBefore(closeTime);
    }

    /** Nhóm hiển thị trên danh sách ca thi của thí sinh (FR-20) và của giảng viên. */
    public SessionPhase phaseAt(LocalDateTime now) {
        if (isUpcoming(now)) {
            return SessionPhase.UPCOMING;
        }
        return isClosed(now) ? SessionPhase.CLOSED : SessionPhase.OPEN;
    }

    public long remainingSeconds(LocalDateTime now) {
        return Math.max(0, Duration.between(now, closeTime).getSeconds());
    }

    /** Ca còn mở nhưng chỉ còn ít hơn ALMOST_OVER. */
    public boolean isAlmostOver(LocalDateTime now) {
        return isOpen(now) && remainingSeconds(now) <= ALMOST_OVER.getSeconds();
    }

    /** Định dạng thời gian còn lại cho đồng hồ đếm ngược: "1:05:09" hoặc "05:09". */
    public static String formatRemaining(long seconds) {
        long h = seconds / 3600;
        long m = seconds % 3600 / 60;
        long s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    /** BR-01 (phần thời gian): ném ngoại lệ kèm lý do cụ thể để báo cho thí sinh. */
    public void ensureOpen(LocalDateTime now) {
        if (isUpcoming(now)) {
            throw new SessionRuleException(SessionRuleException.NOT_OPEN_YET,
                    "Ca thi chưa mở. Giờ mở: " + openTime.format(FMT) + ".");
        }
        if (isClosed(now)) {
            throw new SessionRuleException(SessionRuleException.CLOSED,
                    "Ca thi đã kết thúc lúc " + closeTime.format(FMT) + ".");
        }
    }
}
