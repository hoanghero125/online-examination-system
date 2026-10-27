package com.group2.oes.model;

import com.group2.oes.util.PasswordHasher;

import java.util.Objects;

/**
 * Lớp cha trừu tượng của mọi người dùng.
 * Hai phương thức trừu tượng buộc lớp con tự khai báo vai trò và quyền hạn của mình.
 */
public abstract class User {

    private int userId;
    private final String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private boolean active;

    protected User(int userId, String username, String passwordHash,
                   String fullName, String email, boolean active) {
        this.userId = userId;
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.fullName = fullName;
        this.email = email;
        this.active = active;
    }

    public boolean checkPassword(String raw) {
        return PasswordHasher.matches(raw, passwordHash);
    }

    /** Chỉ gán được một lần, khi DAO vừa thêm người dùng vào CSDL. */
    public void assignId(int id) {
        if (this.userId != 0) {
            throw new IllegalStateException("Người dùng đã có mã " + userId);
        }
        this.userId = id;
    }

    public void changePassword(String newRaw) {
        this.passwordHash = PasswordHasher.hash(newRaw);
    }

    public abstract Role getRole();

    /** Người dùng này có được thao tác trên nội dung của môn học hay không. */
    public abstract boolean canManage(Subject subject);

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User other && userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }

    @Override
    public String toString() {
        return username + " (" + getRole() + ")";
    }
}
