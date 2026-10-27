package com.group2.oes.ui;

import com.group2.oes.model.User;
import com.group2.oes.ui.controller.ChangePasswordController;

/**
 * Những gì màn hình dùng chung (đăng nhập, đổi mật khẩu, cài đặt máy chủ) cần biết về ứng dụng đang chạy.
 *
 * StaffApp và ExamClientApp mỗi lớp tự cài đặt interface này theo cách của mình (đa hình):
 * đăng nhập gọi AuthService hay StudentAuthService, trang chủ là menu quản lý hay danh sách ca thi.
 * Nhờ vậy một tệp login.fxml và một LoginController dùng được cho cả hai ứng dụng,
 * và controller không phải hỏi "đây là ứng dụng nào, người dùng có vai trò gì".
 */
public interface AppShell {

    /** Tên hiện trên màn hình đăng nhập và thanh tiêu đề. */
    String title();

    Navigator navigator();

    /** Gọi trên luồng nền. Ném ExamAppException nếu sai mật khẩu, khóa tài khoản, dùng nhầm ứng dụng. */
    User login(String username, String password);

    /** Gọi trên luồng nền. */
    void changePassword(User user, String oldPassword, String newPassword);

    /** Trang chủ sau khi đăng nhập. Gọi trên luồng giao diện. */
    void showHome(User user);

    /** Màn hình nhập địa chỉ máy chủ CSDL. Gọi trên luồng giao diện. */
    void showServerSetup();

    /** Sau khi đăng nhập thành công: tài khoản bị buộc đổi mật khẩu thì đổi trước, rồi mới vào trang chủ. */
    default void onLoggedIn(User user) {
        UserSession.start(user);
        if (user.isMustChangePassword()) {
            navigator().show("change-password.fxml", (ChangePasswordController c) -> c.init(this, user, true));
        } else {
            showHome(user);
        }
    }

    default void showLogin() {
        navigator().showLogin(this);
    }

    default void logout() {
        UserSession.end();
        showLogin();
    }
}
