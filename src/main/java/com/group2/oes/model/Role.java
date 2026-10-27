package com.group2.oes.model;

public enum Role {
    ADMIN("Quản trị viên"),
    INSTRUCTOR("Giảng viên"),
    STUDENT("Thí sinh");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    /** Tên hiển thị trên giao diện. */
    public String displayName() {
        return displayName;
    }
}
