package com.group2.oes.model;

import java.util.Objects;

/** Lớp học phần: nhóm sinh viên học cùng một môn trong một học kỳ. */
public class CourseClass {

    private final int classId;
    private final String code;
    private final Subject subject;
    private final String semester;

    public CourseClass(int classId, String code, Subject subject, String semester) {
        this.classId = classId;
        this.code = Objects.requireNonNull(code);
        this.subject = Objects.requireNonNull(subject);
        this.semester = semester;
    }

    public int getClassId() {
        return classId;
    }

    public String getCode() {
        return code;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getSemester() {
        return semester;
    }

    @Override
    public String toString() {
        return code;
    }
}
