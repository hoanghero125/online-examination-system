package com.group2.oes.client;

import com.group2.oes.model.SessionPhase;

/**
 * Một dòng trong danh sách ca thi của thí sinh (FR-20): ca thi, trạng thái so với giờ máy chủ, bài làm (nếu có).
 * Quy tắc bật nút "Vào thi" / "Xem kết quả" đặt ở đây để màn hình chỉ việc đọc.
 *
 * @param attempt bài làm của thí sinh trong ca này, null nếu chưa vào thi
 */
public record SessionEntry(SessionInfo session, SessionPhase phase, AttemptState attempt) {

    /** Ca đang mở và thí sinh chưa nộp bài (chưa vào, hoặc đang làm dở). */
    public boolean canEnter() {
        return phase == SessionPhase.OPEN && (attempt == null || !attempt.isLocked());
    }

    public boolean isResuming() {
        return attempt != null && !attempt.isLocked();
    }

    /** Đã nộp bài (kể cả khi ca chưa đóng). */
    public boolean canViewResult() {
        return attempt != null && attempt.isLocked();
    }
}
