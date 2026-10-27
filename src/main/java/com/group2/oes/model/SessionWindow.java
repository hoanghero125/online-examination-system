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
