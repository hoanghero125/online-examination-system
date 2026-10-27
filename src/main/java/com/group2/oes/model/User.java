package com.group2.oes.model;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.util.PasswordHasher;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Lớp cha trừu tượng của mọi người dùng.
 * Hai phương thức trừu tượng buộc lớp con tự khai báo vai trò và quyền hạn của mình.
 */
public abstract class User {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private int userId;
    private final String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private boolean active;
    private boolean mustChangePassword;   // tài khoản nhập từ CSV, mật khẩu mặc định: phải đổi ở lần đăng nhập đầu

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

    /** Đặt mật khẩu mới (đã kiểm tra độ dài). Đổi xong thì không còn bị buộc đổi mật khẩu nữa. */
    public void changePassword(String newRaw) {
        applyNewPasswordHash(hashNewPassword(newRaw));
    }

    /**
     * Kiểm tra quy tắc mật khẩu (FR-04: ít nhất 6 ký tự) rồi băm, chưa gán vào đối tượng.
     * Tách làm hai bước cho phía thí sinh: CSDL phải nhận mã băm mới trước, rồi mới gán vào đối tượng.
     */
    public static String hashNewPassword(String newRaw) {
        ValidationException.require(newRaw != null && newRaw.length() >= MIN_PASSWORD_LENGTH, "PASSWORD_TOO_SHORT",
                "Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
        return PasswordHasher.hash(newRaw);
    }

    /** Gán mã băm mới (do hashNewPassword sinh ra) và bỏ cờ buộc đổi mật khẩu. */
    public void applyNewPasswordHash(String newHash) {
        this.passwordHash = Objects.requireNonNull(newHash);
        this.mustChangePassword = false;
    }

    /** Quản trị viên đặt lại mật khẩu: người dùng phải đổi mật khẩu ở lần đăng nhập sau. */
    public void resetPassword(String temporaryRaw) {
        changePassword(temporaryRaw);
        this.mustChangePassword = true;
    }

    /** Email không bắt buộc; nếu có thì phải đúng dạng "ten@mien.xx". */
    public static boolean isValidEmail(String email) {
        return email == null || email.isBlank() || EMAIL.matcher(email.strip()).matches();
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /** Dùng khi DAO dựng lại người dùng từ CSDL, hoặc khi nhập tài khoản từ CSV. */
    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
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
