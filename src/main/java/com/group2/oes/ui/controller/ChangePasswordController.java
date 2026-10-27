package com.group2.oes.ui.controller;

import com.group2.oes.model.User;
import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/**
 * Đổi mật khẩu (FR-04). Dùng cho cả hai ứng dụng, và cho lần đăng nhập đầu bị buộc đổi mật khẩu
 * (tài khoản nhập từ CSV, hoặc vừa được quản trị viên đặt lại mật khẩu).
 */
public class ChangePasswordController {

    @FXML
    private Label headerLabel;
    @FXML
    private Label noteLabel;
    @FXML
    private PasswordField oldField;
    @FXML
    private PasswordField newField;
    @FXML
    private PasswordField confirmField;
    @FXML
    private Button cancelButton;
    @FXML
    private VBox form;

    private AppShell shell;
    private User user;
    private boolean forced;

    /** @param forced true: bắt buộc đổi trước khi vào ứng dụng; nút Hủy thành Đăng xuất */
    public void init(AppShell shell, User user, boolean forced) {
        this.shell = shell;
        this.user = user;
        this.forced = forced;
        if (forced) {
            headerLabel.setText("Đổi mật khẩu trước khi tiếp tục");
            noteLabel.setText("Tài khoản của bạn đang dùng mật khẩu do hệ thống cấp. Hãy đặt mật khẩu mới để tiếp tục.");
            cancelButton.setText("Đăng xuất");
        }
    }

    @FXML
    private void onSave() {
        String oldPw = oldField.getText();
        String newPw = newField.getText();
        String confirm = confirmField.getText();
        BackgroundTasks.runVoid(form, () -> {
            User.requireConfirmed(newPw, confirm);
            shell.changePassword(user, oldPw, newPw);
        }, () -> {
            ErrorDialogs.info("Đổi mật khẩu", "Đã đổi mật khẩu.");
            shell.showHome(user);
        });
    }

    @FXML
    private void onCancel() {
        if (forced) {
            shell.logout();
        } else {
            shell.showHome(user);
        }
    }
}
