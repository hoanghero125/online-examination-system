package com.group2.oes.model;

/** Trạng thái của ca thi so với giờ hiện tại: sắp diễn ra, đang mở, đã kết thúc. */
public enum SessionPhase {
    UPCOMING("Sắp diễn ra"),
    OPEN("Đang mở"),
    CLOSED("Đã kết thúc");

    private final String displayName;

    SessionPhase(String displayName) {
        this.displayName = displayName;
    }

    /** Tên hiển thị trên giao diện. */
    public String displayName() {
        return displayName;
    }
}
