package com.group2.oes.model;

public enum Difficulty {
    EASY("Dễ"),
    MEDIUM("Trung bình"),
    HARD("Khó");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    /** Tên hiển thị trên giao diện. */
    public String displayName() {
        return displayName;
    }
}
