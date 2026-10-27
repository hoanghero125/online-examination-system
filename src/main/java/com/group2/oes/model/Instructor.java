package com.group2.oes.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Instructor extends User {

    // Set vì một môn chỉ được phân công một lần; LinkedHashSet để giữ thứ tự hiển thị.
    private final Set<Subject> subjects = new LinkedHashSet<>();

    public Instructor(int userId, String username, String passwordHash,
                      String fullName, String email, boolean active) {
        super(userId, username, passwordHash, fullName, email, active);
    }

    public void assignSubject(Subject subject) {
        subjects.add(subject);
    }

    public void unassignSubject(Subject subject) {
        subjects.remove(subject);
    }

    /** Trả về bản chỉ đọc để lớp ngoài không sửa được danh sách phân công. */
    public Set<Subject> getSubjects() {
        return Collections.unmodifiableSet(subjects);
    }

    @Override
    public Role getRole() {
        return Role.INSTRUCTOR;
    }

    /** Giảng viên chỉ quản lý môn mình được phân công. */
    @Override
    public boolean canManage(Subject subject) {
        return subjects.contains(subject);
    }
}
