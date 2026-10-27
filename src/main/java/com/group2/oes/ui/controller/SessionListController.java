package com.group2.oes.ui.controller;

import com.group2.oes.app.ClientContext;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.ErrorDialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Trang chủ ứng dụng thi: danh sách ca thi của thí sinh (FR-20). */
public class SessionListController {

    @FXML
    private Label userLabel;

    private AppShell shell;
    private ClientContext client;
    private User user;

    public void init(AppShell shell, ClientContext client, User user) {
        this.shell = shell;
        this.client = client;
        this.user = user;
        String code = user instanceof Student s ? " (" + s.getStudentCode() + ")" : "";
        userLabel.setText(user.getFullName() + code);
    }

    @FXML
    private void onChangePassword() {
        shell.navigator().show("change-password.fxml", (ChangePasswordController c) -> c.init(shell, user, false));
    }

    @FXML
    private void onLogout() {
        if (ErrorDialogs.confirm("Đăng xuất", "Bạn có chắc muốn đăng xuất?", "Đăng xuất")) {
            shell.logout();
        }
    }
}
