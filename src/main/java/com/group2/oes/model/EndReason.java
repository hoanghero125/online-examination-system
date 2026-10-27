package com.group2.oes.model;

/** Lý do một bài làm bị kết thúc. */
public enum EndReason {
    MANUAL("Thí sinh tự nộp"),
    TIME_OUT("Hết giờ, tự động nộp"),
    VIOLATION("Kết thúc do vi phạm quy chế");   // rời cửa sổ làm bài đủ 5 lần

    private final String displayName;

    EndReason(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
