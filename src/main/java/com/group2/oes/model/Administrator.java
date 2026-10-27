package com.group2.oes.model;

public class Administrator extends User {

    public Administrator(int userId, String username, String passwordHash,
                         String fullName, String email, boolean active) {
        super(userId, username, passwordHash, fullName, email, active);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    /** Quản trị viên thao tác được trên mọi môn học. */
    @Override
    public boolean canManage(Subject subject) {
        return true;
    }
}
