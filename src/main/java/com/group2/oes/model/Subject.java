package com.group2.oes.model;

import java.util.Objects;

public class Subject {

    private final int subjectId;
    private final String code;
    private final String name;

    public Subject(int subjectId, String code, String name) {
        this.subjectId = subjectId;
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
    }

    public int getSubjectId() {
        return subjectId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    // Hai môn học bằng nhau khi trùng mã. Cần thiết để List.contains / Set hoạt động đúng.
    @Override
    public boolean equals(Object o) {
        return o instanceof Subject other && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}
