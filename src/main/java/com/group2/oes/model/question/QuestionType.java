package com.group2.oes.model.question;

public enum QuestionType {
    SINGLE_CHOICE("Một đáp án"),
    MULTI_CHOICE("Nhiều đáp án"),
    TRUE_FALSE("Đúng/Sai"),
    FILL_IN_BLANK("Điền khuyết");

    private final String displayName;

    QuestionType(String displayName) {
        this.displayName = displayName;
    }

    /** Tên hiển thị trên giao diện. */
    public String displayName() {
        return displayName;
    }
}
