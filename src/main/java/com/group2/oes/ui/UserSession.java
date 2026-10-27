package com.group2.oes.ui;

import com.group2.oes.model.User;

/**
 * Người dùng đang đăng nhập trên máy này. Đây là trạng thái toàn cục duy nhất của giao diện:
 * mọi màn hình đều cần biết "ai đang thao tác" để truyền vào service kiểm tra quyền.
 */
public final class UserSession {

    private static User current;

    private UserSession() {
    }

    static synchronized void start(User user) {
        current = user;
    }

    /** @throws IllegalStateException nếu chưa đăng nhập (lỗi lập trình: màn hình mở sai thứ tự) */
    public static synchronized User get() {
        if (current == null) {
            throw new IllegalStateException("Chưa có người dùng đăng nhập");
        }
        return current;
    }

    public static synchronized boolean isLoggedIn() {
        return current != null;
    }

    static synchronized void end() {
        current = null;
    }
}
