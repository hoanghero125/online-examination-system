package com.group2.oes.model;

/** Cách chấm câu nhiều đáp án, cấu hình theo từng đề thi. */
public enum MultiChoicePolicy {
    ALL_OR_NOTHING("Toàn phần: đúng hết mới có điểm"),
    PARTIAL("Theo tỷ lệ: đúng phần nào được phần đó");

    private final String displayName;

    MultiChoicePolicy(String displayName) {
        this.displayName = displayName;
    }

    /** Tên hiển thị trên giao diện. */
    public String displayName() {
        return displayName;
    }
}
