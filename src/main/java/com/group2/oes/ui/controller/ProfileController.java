package com.group2.oes.ui.controller;

import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.StaffWorkspace;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Hồ sơ cá nhân (FR-04): họ tên, email; đổi mật khẩu dùng màn hình chung. */
public class ProfileController {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private VBox form;

    private StaffWorkspace ws;
    private AppShell shell;

    public void init(StaffWorkspace ws, AppShell shell) {
        this.ws = ws;
        this.shell = shell;
        usernameLabel.setText(ws.user().getUsername());
        roleLabel.setText(ws.user().getRole().displayName());
        fullNameField.setText(ws.user().getFullName());
        emailField.setText(ws.user().getEmail() == null ? "" : ws.user().getEmail());
    }

    @FXML
    private void onSave() {
        String name = fullNameField.getText();
        String email = emailField.getText();
        BackgroundTasks.runVoid(form, () -> ws.app().auth.updateProfile(ws.user(), name, email),
                () -> ErrorDialogs.info("Hồ sơ", "Đã lưu thông tin."));
    }

    @FXML
    private void onChangePassword() {
        shell.navigator().show("change-password.fxml",
                (ChangePasswordController c) -> c.init(shell, ws.user(), false));
    }
}
