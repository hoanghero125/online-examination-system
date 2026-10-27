package com.group2.oes.model;

import java.util.Objects;

public class Student extends User {

    private final String studentCode;
    private CourseClass courseClass;

    public Student(int userId, String username, String passwordHash,
                   String fullName, String email, boolean active, String studentCode) {
        super(userId, username, passwordHash, fullName, email, active);
        this.studentCode = Objects.requireNonNull(studentCode);
    }

    public String getStudentCode() {
        return studentCode;
    }

    public CourseClass getCourseClass() {
        return courseClass;
    }

    public void setCourseClass(CourseClass courseClass) {
        this.courseClass = courseClass;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    /** Thí sinh không quản lý nội dung học thuật. */
    @Override
    public boolean canManage(Subject subject) {
        return false;
    }
}
